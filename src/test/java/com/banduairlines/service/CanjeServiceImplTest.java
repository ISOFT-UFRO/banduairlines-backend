package com.banduairlines.service;

import com.banduairlines.dto.cuenta.ActualizacionFidelizacion;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.exception.ServicioExternoException;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.model.Beneficio;
import com.banduairlines.model.CuentaFidelizacion;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.persistence.cuenta.RegistroPuntos;
import com.banduairlines.persistence.cuenta.TipoRegistroPuntos;
import com.banduairlines.repository.BeneficioRepository;
import com.banduairlines.repository.CuentaPuntosRepository;
import com.banduairlines.repository.HistorialPuntosRepository;
import com.banduairlines.repository.RegistroPuntosRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.cuenta.ActualizacionFidelizacionService;
import com.banduairlines.service.cuenta.CanjeService.ResultadoCanje;
import com.banduairlines.service.cuenta.CodigosFidelizacion;
import com.banduairlines.service.cuenta.EvaluadorBeneficio;
import com.banduairlines.service.impl.CanjeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CanjeServiceImplTest {

    private static final Long ID_USUARIO = 7L;
    private static final Long ID_BENEFICIO = 3L;
    private static final String CLAVE = "clave-prueba-1";
    private static final String PREFIJO = "CANJE:clave-prueba-1:";
    private static final Instant AHORA = Instant.parse("2026-10-16T15:00:00Z");

    @Mock
    private CuentaPuntosRepository cuentas;
    @Mock
    private BeneficioRepository beneficios;
    @Mock
    private HistorialPuntosRepository historial;
    @Mock
    private RegistroPuntosRepository registros;
    @Mock
    private ActualizacionFidelizacionService actualizacion;
    @Mock
    private UsuarioActual usuarioActual;

    private CanjeServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new CanjeServiceImpl(cuentas, beneficios, historial, registros, actualizacion,
                new EvaluadorBeneficio(), usuarioActual, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void canjeCorrecto_registraMovimientoCanjeYDescuentaPuntos() {
        Usuario pasajero = usuario(RolUsuario.PASAJERO);
        prepararCuentaSinCanjePrevio(pasajero);
        when(beneficios.findById(ID_BENEFICIO)).thenReturn(Optional.of(beneficio(Nivel.PLATA, 200, true)));
        when(actualizacion.actualizarActual()).thenReturn(estado(Nivel.PLATA, 1000));
        when(registros.save(any(RegistroPuntos.class))).thenAnswer(invocacion -> {
            RegistroPuntos registro = invocacion.getArgument(0);
            ReflectionTestUtils.setField(registro, "idRegistro", 50L);
            return registro;
        });

        ResultadoCanje resultado = servicio.canjearActual(ID_BENEFICIO, CLAVE);

        assertTrue(resultado.creado());
        assertEquals(50L, resultado.canje().idCanje());
        assertEquals(200, resultado.canje().puntosCanjeados());
        assertEquals(800, resultado.canje().saldoPuntos());
        assertEquals("Sala VIP", resultado.canje().nombreBeneficio());

        ArgumentCaptor<RegistroPuntos> captor = ArgumentCaptor.forClass(RegistroPuntos.class);
        verify(registros).save(captor.capture());
        RegistroPuntos guardado = captor.getValue();
        assertEquals(TipoRegistroPuntos.CANJE, guardado.getTipo());
        assertEquals(200, guardado.getCantidad());
        assertEquals(ID_USUARIO, guardado.getIdUsuario());
        assertEquals("CANJE:clave-prueba-1:B3", guardado.getReferencia());
        assertEquals(AHORA, guardado.getFecha());
        assertNull(guardado.getCodigoPNR());
    }

    @Test
    void canje_noModificaMillasNiNivel() {
        Usuario pasajero = usuario(RolUsuario.PASAJERO);
        CuentaFidelizacion cuenta = CuentaFidelizacion.builder()
                .usuario(pasajero).saldoMillas(12000).nivel(Nivel.PLATA).build();
        pasajero.setCuentaFidelizacion(cuenta);
        prepararCuentaSinCanjePrevio(pasajero);
        when(beneficios.findById(ID_BENEFICIO)).thenReturn(Optional.of(beneficio(Nivel.BASICO, 200, true)));
        when(actualizacion.actualizarActual()).thenReturn(estado(Nivel.PLATA, 1000));
        when(registros.save(any(RegistroPuntos.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        servicio.canjearActual(ID_BENEFICIO, CLAVE);

        assertEquals(12000, cuenta.getSaldoMillas());
        assertEquals(Nivel.PLATA, cuenta.getNivel());
    }

    @Test
    void puntosInsuficientes_rechazaSinRegistrar() {
        prepararCuentaSinCanjePrevio(usuario(RolUsuario.PASAJERO));
        when(beneficios.findById(ID_BENEFICIO)).thenReturn(Optional.of(beneficio(Nivel.BASICO, 500, true)));
        when(actualizacion.actualizarActual()).thenReturn(estado(Nivel.BASICO, 499));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.canjearActual(ID_BENEFICIO, CLAVE));

        assertEquals(CodigosFidelizacion.PUNTOS_INSUFICIENTES, error.getCodigo());
        verify(registros, never()).save(any());
    }

    @Test
    void nivelInsuficiente_rechazaSinRegistrar() {
        prepararCuentaSinCanjePrevio(usuario(RolUsuario.PASAJERO));
        when(beneficios.findById(ID_BENEFICIO)).thenReturn(Optional.of(beneficio(Nivel.ORO, 100, true)));
        when(actualizacion.actualizarActual()).thenReturn(estado(Nivel.PLATA, 5000));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.canjearActual(ID_BENEFICIO, CLAVE));

        assertEquals(CodigosFidelizacion.NIVEL_INSUFICIENTE, error.getCodigo());
        verify(registros, never()).save(any());
    }

    @Test
    void beneficioNoDisponible_rechazaSinRegistrar() {
        prepararCuentaSinCanjePrevio(usuario(RolUsuario.PASAJERO));
        when(beneficios.findById(ID_BENEFICIO)).thenReturn(Optional.of(beneficio(Nivel.BASICO, 100, false)));
        when(actualizacion.actualizarActual()).thenReturn(estado(Nivel.ORO, 5000));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.canjearActual(ID_BENEFICIO, CLAVE));

        assertEquals(CodigosFidelizacion.BENEFICIO_AGOTADO, error.getCodigo());
        verify(registros, never()).save(any());
    }

    @Test
    void beneficioInexistente_responde404() {
        prepararCuentaSinCanjePrevio(usuario(RolUsuario.PASAJERO));
        when(beneficios.findById(ID_BENEFICIO)).thenReturn(Optional.empty());

        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.canjearActual(ID_BENEFICIO, CLAVE));

        assertEquals(CodigosFidelizacion.BENEFICIO_NO_ENCONTRADO, error.getCodigo());
        verify(registros, never()).save(any());
    }

    @Test
    void claveRepetidaConMismoBeneficio_devuelveCanjeOriginalSinDescontarOtraVez() {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(cuentas.bloquear(ID_USUARIO)).thenReturn(Optional.of(usuario(RolUsuario.PASAJERO)));
        RegistroPuntos anterior = registro(50L, TipoRegistroPuntos.CANJE, 200, "CANJE:clave-prueba-1:B3");
        when(historial.findFirstByIdUsuarioAndTipoAndReferenciaStartingWith(ID_USUARIO, TipoRegistroPuntos.CANJE, PREFIJO))
                .thenReturn(Optional.of(anterior));
        when(beneficios.findById(ID_BENEFICIO)).thenReturn(Optional.of(beneficio(Nivel.BASICO, 200, true)));
        when(registros.calcularSaldo(ID_USUARIO)).thenReturn(800L);

        ResultadoCanje resultado = servicio.canjearActual(ID_BENEFICIO, CLAVE);

        assertFalse(resultado.creado());
        assertEquals(50L, resultado.canje().idCanje());
        assertEquals(800, resultado.canje().saldoPuntos());
        verify(registros, never()).save(any());
        verifyNoInteractions(actualizacion);
    }

    @Test
    void claveRepetidaConOtroBeneficio_respondeCanjeDuplicado() {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(cuentas.bloquear(ID_USUARIO)).thenReturn(Optional.of(usuario(RolUsuario.PASAJERO)));
        RegistroPuntos anterior = registro(50L, TipoRegistroPuntos.CANJE, 200, "CANJE:clave-prueba-1:B9");
        when(historial.findFirstByIdUsuarioAndTipoAndReferenciaStartingWith(ID_USUARIO, TipoRegistroPuntos.CANJE, PREFIJO))
                .thenReturn(Optional.of(anterior));

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.canjearActual(ID_BENEFICIO, CLAVE));

        assertEquals(CodigosFidelizacion.CANJE_DUPLICADO, error.getCodigo());
        verify(registros, never()).save(any());
    }

    @Test
    void sinClaveIdempotencia_rechazaAntesDeTocarLaCuenta() {
        assertThrows(ValidacionException.class, () -> servicio.canjearActual(ID_BENEFICIO, null));
        verifyNoInteractions(cuentas, registros, actualizacion);
    }

    @Test
    void administrador_noParticipaEnFidelizacion() {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(cuentas.bloquear(ID_USUARIO)).thenReturn(Optional.of(usuario(RolUsuario.ADMINISTRADOR)));

        assertThrows(AccessDeniedException.class, () -> servicio.canjearActual(ID_BENEFICIO, CLAVE));
        verify(registros, never()).save(any());
    }

    @Test
    void millasNoDisponibles_sePropagaSinRegistrar() {
        prepararCuentaSinCanjePrevio(usuario(RolUsuario.PASAJERO));
        when(beneficios.findById(ID_BENEFICIO)).thenReturn(Optional.of(beneficio(Nivel.BASICO, 100, true)));
        when(actualizacion.actualizarActual()).thenThrow(
                new ServicioExternoException("Sin historial de vuelos").conCodigo(CodigosFidelizacion.MILLAS_NO_DISPONIBLES));

        ServicioExternoException error = assertThrows(ServicioExternoException.class,
                () -> servicio.canjearActual(ID_BENEFICIO, CLAVE));

        assertEquals(CodigosFidelizacion.MILLAS_NO_DISPONIBLES, error.getCodigo());
        verify(registros, never()).save(any());
    }

    private void prepararCuentaSinCanjePrevio(Usuario usuario) {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(cuentas.bloquear(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(historial.findFirstByIdUsuarioAndTipoAndReferenciaStartingWith(ID_USUARIO, TipoRegistroPuntos.CANJE, PREFIJO))
                .thenReturn(Optional.empty());
    }

    private static Usuario usuario(RolUsuario rol) {
        return Usuario.builder()
                .id(ID_USUARIO)
                .email("pasajero@bandu.cl")
                .passwordHash("hash")
                .nombre("Ana")
                .apellido("Pérez")
                .rol(rol)
                .activo(true)
                .build();
    }

    private static Beneficio beneficio(Nivel nivel, int costo, boolean disponible) {
        return Beneficio.builder()
                .idBeneficio(ID_BENEFICIO)
                .nombre("Sala VIP")
                .nivelRequerido(nivel)
                .costoPuntos(costo)
                .disponible(disponible)
                .build();
    }

    private static ActualizacionFidelizacion estado(Nivel nivel, long saldo) {
        return new ActualizacionFidelizacion(new BigDecimal("12000"), saldo, nivel, BigDecimal.ONE);
    }

    private static RegistroPuntos registro(Long id, TipoRegistroPuntos tipo, long cantidad, String referencia) {
        RegistroPuntos registro = new RegistroPuntos(ID_USUARIO, tipo, cantidad, AHORA, referencia, null);
        ReflectionTestUtils.setField(registro, "idRegistro", id);
        return registro;
    }
}
