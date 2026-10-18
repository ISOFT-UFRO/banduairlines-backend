package com.banduairlines.service;

import com.banduairlines.dto.cuenta.ActualizacionFidelizacion;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.exception.ServicioExternoException;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.model.enums.TipoMovimiento;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.cuenta.ActualizacionFidelizacionService;
import com.banduairlines.service.cuenta.BeneficioConfigurado;
import com.banduairlines.service.cuenta.CanjeService.ResultadoCanje;
import com.banduairlines.service.cuenta.CatalogoBeneficios;
import com.banduairlines.service.cuenta.CodigosFidelizacion;
import com.banduairlines.service.cuenta.EvaluadorBeneficio;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos.CanjeRegistrado;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos.MovimientoPuntos;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos.ResultadoRegistroCanje;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos.SolicitudRegistroCanje;
import com.banduairlines.service.cuenta.PropiedadesFidelizacion;
import com.banduairlines.service.impl.AccesoMovimientosPuntos;
import com.banduairlines.service.impl.CanjeServiceImpl;
import com.banduairlines.service.impl.MapeadorFidelizacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CanjeServiceImplTest {

    private static final Long ID_USUARIO = 7L;
    private static final Long ID_BENEFICIO = 3L;
    private static final String CLAVE = "clave-prueba-1";
    private static final Instant FECHA = Instant.parse("2026-10-16T15:00:00Z");

    @Mock
    private FuenteMovimientosPuntos fuente;
    @Mock
    private AccesoMovimientosPuntos acceso;
    @Mock
    private ActualizacionFidelizacionService actualizacion;
    @Mock
    private UsuarioActual usuarioActual;

    private CanjeServiceImpl servicio(BeneficioConfigurado... catalogo) {
        EvaluadorBeneficio evaluador = new EvaluadorBeneficio();
        return new CanjeServiceImpl(acceso, new CatalogoBeneficios(new PropiedadesFidelizacion(List.of(catalogo))),
                actualizacion, evaluador, usuarioActual, new MapeadorFidelizacion(evaluador));
    }

    private static BeneficioConfigurado salaVip(Nivel nivel, int costo, int stock, boolean habilitado) {
        return new BeneficioConfigurado(ID_BENEFICIO, "Sala VIP", "Acceso a sala VIP", nivel, costo, stock, habilitado);
    }

    @Test
    void canjeCorrecto_registraMovimientoCanjeYDescuentaPuntos() {
        BeneficioConfigurado beneficio = salaVip(Nivel.PLATA, 200, 10, true);
        prepararSinCanjePrevio(Nivel.PLATA, 1000);
        when(fuente.contarCanjes(Set.of(ID_BENEFICIO))).thenReturn(Map.of(ID_BENEFICIO, 4L));
        SolicitudRegistroCanje esperada = new SolicitudRegistroCanje(
                ID_USUARIO, CLAVE, ID_BENEFICIO, "Sala VIP", 200, 10, 1000, 800);
        when(fuente.registrarCanje(esperada)).thenReturn(nuevo(canje(50L, 200, 1000, "Sala VIP")));

        ResultadoCanje resultado = servicio(beneficio).canjearActual(ID_BENEFICIO, CLAVE);

        assertTrue(resultado.creado());
        assertEquals(50L, resultado.canje().idCanje());
        assertEquals(1000, resultado.canje().saldoAnterior());
        assertEquals(200, resultado.canje().puntosDescontados());
        assertEquals(800, resultado.canje().saldoActual());
        assertEquals(-200, resultado.canje().movimiento().cantidadPuntos());
        assertEquals("Canje de Sala VIP", resultado.canje().movimiento().descripcion());

        InOrder orden = inOrder(actualizacion, fuente);
        orden.verify(actualizacion).actualizarActual();
        orden.verify(fuente).buscarCanje(ID_USUARIO, CLAVE);
        orden.verify(fuente).contarCanjes(Set.of(ID_BENEFICIO));
        orden.verify(fuente).registrarCanje(esperada);
    }

    @Test
    void canje_noModificaMillasNiNivel() {
        BeneficioConfigurado beneficio = salaVip(Nivel.BASICO, 500, 10, true);
        prepararSinCanjePrevio(Nivel.BASICO, 500);
        when(fuente.contarCanjes(Set.of(ID_BENEFICIO))).thenReturn(Map.of());
        when(fuente.registrarCanje(any())).thenReturn(nuevo(canje(51L, 500, 500, "Sala VIP")));

        ResultadoCanje resultado = servicio(beneficio).canjearActual(ID_BENEFICIO, CLAVE);

        assertEquals(0, resultado.canje().saldoActual());
        verify(actualizacion).actualizarActual();
        verifyNoMoreInteractions(actualizacion);
    }

    @Test
    void beneficioNoDisponible_rechazaSinRegistrar() {
        prepararSinCanjePrevio(Nivel.ORO, 9999);
        when(fuente.contarCanjes(Set.of(ID_BENEFICIO))).thenReturn(Map.of(ID_BENEFICIO, 5L));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio(salaVip(Nivel.BASICO, 100, 5, true)).canjearActual(ID_BENEFICIO, CLAVE));

        assertEquals(CodigosFidelizacion.BENEFICIO_AGOTADO, error.getCodigo());
        verify(fuente, never()).registrarCanje(any());
    }

    @Test
    void nivelInsuficiente_rechazaSinRegistrar() {
        prepararSinCanjePrevio(Nivel.PLATA, 5000);
        when(fuente.contarCanjes(Set.of(ID_BENEFICIO))).thenReturn(Map.of());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio(salaVip(Nivel.ORO, 100, 5, true)).canjearActual(ID_BENEFICIO, CLAVE));

        assertEquals(CodigosFidelizacion.NIVEL_INSUFICIENTE, error.getCodigo());
        verify(fuente, never()).registrarCanje(any());
    }

    @Test
    void puntosInsuficientes_rechazaSinRegistrar() {
        prepararSinCanjePrevio(Nivel.BASICO, 499);
        when(fuente.contarCanjes(Set.of(ID_BENEFICIO))).thenReturn(Map.of());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio(salaVip(Nivel.BASICO, 500, 5, true)).canjearActual(ID_BENEFICIO, CLAVE));

        assertEquals(CodigosFidelizacion.PUNTOS_INSUFICIENTES, error.getCodigo());
        verify(fuente, never()).registrarCanje(any());
    }

    @Test
    void beneficioInexistente_responde404() {
        prepararSinCanjePrevio(Nivel.BASICO, 1000);

        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> servicio().canjearActual(ID_BENEFICIO, CLAVE));

        assertEquals(CodigosFidelizacion.BENEFICIO_NO_ENCONTRADO, error.getCodigo());
        verify(fuente, never()).contarCanjes(any());
        verify(fuente, never()).registrarCanje(any());
    }

    @Test
    void claveRepetidaConMismoBeneficio_devuelveCanjeOriginalSinDescontarOtraVez() {
        prepararCanjePrevio(canje(50L, 200, 1000, "Sala VIP"));

        ResultadoCanje resultado = servicio().canjearActual(ID_BENEFICIO, CLAVE);

        assertFalse(resultado.creado());
        assertEquals(50L, resultado.canje().idCanje());
        assertEquals(1000, resultado.canje().saldoAnterior());
        assertEquals(800, resultado.canje().saldoActual());
        assertEquals(FECHA, resultado.canje().movimiento().fecha());
        assertEquals("Canje de Sala VIP", resultado.canje().movimiento().descripcion());
        verify(fuente, never()).contarCanjes(any());
        verify(fuente, never()).registrarCanje(any());
    }

    @Test
    void claveRepetidaConOtroBeneficio_respondeCanjeDuplicado() {
        prepararCanjePrevio(canje(50L, 200, 1000, "Sala VIP"));

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio(salaVip(Nivel.BASICO, 200, 10, true)).canjearActual(9L, CLAVE));

        assertEquals(CodigosFidelizacion.CANJE_DUPLICADO, error.getCodigo());
        verify(fuente, never()).registrarCanje(any());
    }

    @Test
    void sinClaveIdempotencia_rechazaAntesDeTocarLaCuenta() {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);

        assertThrows(ValidacionException.class,
                () -> servicio(salaVip(Nivel.BASICO, 100, 5, true)).canjearActual(ID_BENEFICIO, null));
        verify(fuente, never()).buscarCanje(any(), anyString());
        verifyNoInteractions(acceso, actualizacion);
    }

    @Test
    void administrador_noParticipaEnFidelizacion() {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(acceso.exigir()).thenReturn(fuente);
        when(actualizacion.actualizarActual()).thenThrow(new AccessDeniedException("Solo pasajeros"));

        assertThrows(AccessDeniedException.class,
                () -> servicio(salaVip(Nivel.BASICO, 100, 5, true)).canjearActual(ID_BENEFICIO, CLAVE));
        verifyNoInteractions(fuente);
    }

    @Test
    void millasNoDisponibles_sePropagaSinRegistrar() {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(acceso.exigir()).thenReturn(fuente);
        when(actualizacion.actualizarActual()).thenThrow(new ServicioExternoException("Sin historial de vuelos")
                .conCodigo(CodigosFidelizacion.MILLAS_NO_DISPONIBLES));

        ServicioExternoException error = assertThrows(ServicioExternoException.class,
                () -> servicio(salaVip(Nivel.BASICO, 100, 5, true)).canjearActual(ID_BENEFICIO, CLAVE));

        assertEquals(CodigosFidelizacion.MILLAS_NO_DISPONIBLES, error.getCodigo());
        verifyNoInteractions(fuente);
    }

    private void prepararSinCanjePrevio(Nivel nivel, long saldo) {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(acceso.exigir()).thenReturn(fuente);
        when(actualizacion.actualizarActual()).thenReturn(estado(nivel, saldo));
        when(fuente.buscarCanje(ID_USUARIO, CLAVE)).thenReturn(Optional.empty());
    }

    private void prepararCanjePrevio(CanjeRegistrado anterior) {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(acceso.exigir()).thenReturn(fuente);
        when(actualizacion.actualizarActual()).thenReturn(estado(Nivel.BASICO, 800));
        when(fuente.buscarCanje(ID_USUARIO, CLAVE)).thenReturn(Optional.of(anterior));
    }

    private static ResultadoRegistroCanje nuevo(CanjeRegistrado canje) {
        return new ResultadoRegistroCanje(canje, true);
    }

    private static ActualizacionFidelizacion estado(Nivel nivel, long saldo) {
        return new ActualizacionFidelizacion(new BigDecimal("12000"), saldo, nivel, BigDecimal.ONE);
    }

    private static CanjeRegistrado canje(Long id, long costo, long saldoAnterior, String nombre) {
        MovimientoPuntos movimiento = new MovimientoPuntos(id, TipoMovimiento.CANJE, costo, FECHA,
                "Canje de " + nombre, null, ID_BENEFICIO);
        return new CanjeRegistrado(movimiento, nombre, saldoAnterior, saldoAnterior - costo);
    }
}
