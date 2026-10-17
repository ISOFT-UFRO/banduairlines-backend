package com.banduairlines.service;

import com.banduairlines.dto.cuenta.ActualizacionFidelizacion;
import com.banduairlines.dto.cuenta.ConfiguracionNivel;
import com.banduairlines.dto.cuenta.RespuestaFidelizacion;
import com.banduairlines.dto.cuenta.RespuestaMovimientoPuntos;
import com.banduairlines.dto.cuenta.RespuestaPaginada;
import com.banduairlines.exception.ServicioExternoException;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.model.enums.TipoMovimiento;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.cuenta.ActualizacionFidelizacionService;
import com.banduairlines.service.cuenta.BeneficioConfigurado;
import com.banduairlines.service.cuenta.CatalogoBeneficios;
import com.banduairlines.service.cuenta.CodigosFidelizacion;
import com.banduairlines.service.cuenta.EvaluadorBeneficio;
import com.banduairlines.service.cuenta.GestionNivelService;
import com.banduairlines.service.cuenta.PropiedadesFidelizacion;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos.MovimientoPuntos;
import com.banduairlines.service.impl.AccesoMovimientosPuntos;
import com.banduairlines.service.impl.FidelizacionServiceImpl;
import com.banduairlines.service.impl.MapeadorFidelizacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FidelizacionServiceImplTest {

    private static final Long ID_USUARIO = 7L;
    private static final Instant FECHA = Instant.parse("2026-10-16T15:00:00Z");

    @Mock
    private ActualizacionFidelizacionService actualizacion;
    @Mock
    private GestionNivelService niveles;
    @Mock
    private FuenteMovimientosPuntos fuente;
    @Mock
    private AccesoMovimientosPuntos acceso;
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private UsuarioActual usuarioActual;

    private FidelizacionServiceImpl servicio;

    @BeforeEach
    void setUp() {
        CatalogoBeneficios catalogo = new CatalogoBeneficios(new PropiedadesFidelizacion(List.of(
                new BeneficioConfigurado(1L, "Equipaje extra", null, Nivel.BASICO, 100, 10, true),
                new BeneficioConfigurado(2L, "Sala VIP", null, Nivel.ORO, 100, 10, true),
                new BeneficioConfigurado(3L, "Upgrade", null, Nivel.PLATA, 5000, 10, true))));
        servicio = new FidelizacionServiceImpl(actualizacion, niveles, catalogo, acceso, usuarios, usuarioActual,
                new MapeadorFidelizacion(new EvaluadorBeneficio()));
    }

    @Test
    void consultaFidelizacion_armaResumenConProgresoBeneficiosYMovimientos() {
        prepararResumen();
        when(actualizacion.actualizarActual()).thenReturn(new ActualizacionFidelizacion(
                new BigDecimal("12500.500000"), 900, Nivel.PLATA, new BigDecimal("1.2")));
        when(niveles.obtenerConfiguracion(Nivel.PLATA))
                .thenReturn(new ConfiguracionNivel(Nivel.PLATA, 10000, new BigDecimal("1.2")));
        when(niveles.obtenerConfiguracion(Nivel.ORO))
                .thenReturn(new ConfiguracionNivel(Nivel.ORO, 25000, new BigDecimal("1.5")));
        when(fuente.consultarMovimientos(ID_USUARIO, PageRequest.of(0, 5))).thenReturn(new PageImpl<>(List.of(
                movimiento(40L, TipoMovimiento.ACUMULACION, 300, "Acumulación por vuelo 4", null))));

        RespuestaFidelizacion respuesta = servicio.consultarActual();

        assertEquals(900, respuesta.puntosDisponibles());
        assertEquals(Nivel.PLATA, respuesta.nivelActual());
        assertEquals(new BigDecimal("1.2"), respuesta.multiplicadorPuntos());
        assertEquals(Nivel.ORO, respuesta.progreso().nivelSiguiente());
        assertEquals(25000L, respuesta.progreso().millasNivelSiguiente());
        assertEquals(12500, respuesta.progreso().millasFaltantes());
        assertEquals(16, respuesta.progreso().porcentaje());
        assertEquals(List.of(1L), respuesta.beneficiosDisponibles().stream().map(b -> b.idBeneficio()).toList());
        assertEquals(1, respuesta.movimientosRecientes().size());
        assertEquals(300, respuesta.movimientosRecientes().get(0).cantidadPuntos());
        verify(fuente).consultarMovimientos(ID_USUARIO, PageRequest.of(0, 5));
    }

    @Test
    void enNivelOro_elProgresoEstaCompleto() {
        prepararResumen();
        when(actualizacion.actualizarActual()).thenReturn(new ActualizacionFidelizacion(
                new BigDecimal("40000"), 0, Nivel.ORO, new BigDecimal("1.5")));
        when(fuente.consultarMovimientos(ID_USUARIO, PageRequest.of(0, 5))).thenReturn(new PageImpl<>(List.of()));

        RespuestaFidelizacion respuesta = servicio.consultarActual();

        assertNull(respuesta.progreso().nivelSiguiente());
        assertNull(respuesta.progreso().millasNivelSiguiente());
        assertEquals(0, respuesta.progreso().millasFaltantes());
        assertEquals(100, respuesta.progreso().porcentaje());
        verifyNoInteractions(niveles);
    }

    @Test
    void consultaFidelizacion_propagaMillasNoDisponibles() {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(actualizacion.actualizarActual()).thenThrow(
                new ServicioExternoException("Sin historial").conCodigo(CodigosFidelizacion.MILLAS_NO_DISPONIBLES));

        ServicioExternoException error = assertThrows(ServicioExternoException.class, () -> servicio.consultarActual());

        assertEquals(CodigosFidelizacion.MILLAS_NO_DISPONIBLES, error.getCodigo());
    }

    @Test
    void historial_esPaginadoYMuestraElBeneficioCanjeado() {
        prepararHistorial();
        PageRequest pagina = PageRequest.of(1, 2);
        when(fuente.consultarMovimientos(ID_USUARIO, pagina)).thenReturn(new PageImpl<>(List.of(
                movimiento(51L, TipoMovimiento.CANJE, 200, "Canje de Sala VIP", 3L),
                movimiento(40L, TipoMovimiento.ACUMULACION, 300, "Acumulación por vuelo 4", null)), pagina, 5));

        RespuestaPaginada<RespuestaMovimientoPuntos> respuesta = servicio.listarMovimientosActual(1, 2);

        assertEquals(1, respuesta.pagina());
        assertEquals(2, respuesta.tamano());
        assertEquals(5, respuesta.totalElementos());
        assertEquals(3, respuesta.totalPaginas());
        RespuestaMovimientoPuntos canje = respuesta.contenido().get(0);
        assertEquals(-200, canje.cantidadPuntos());
        assertEquals(3L, canje.idBeneficio());
        assertEquals("Canje de Sala VIP", canje.descripcion());
        RespuestaMovimientoPuntos acumulacion = respuesta.contenido().get(1);
        assertEquals(300, acumulacion.cantidadPuntos());
        assertNull(acumulacion.idBeneficio());
    }

    @Test
    void historial_rechazaTamanoFueraDeRango() {
        assertThrows(ValidacionException.class, () -> servicio.listarMovimientosActual(0, 0));
        assertThrows(ValidacionException.class, () -> servicio.listarMovimientosActual(0, 101));
        assertThrows(ValidacionException.class, () -> servicio.listarMovimientosActual(-1, 20));
        verifyNoInteractions(acceso, usuarios);
    }

    @Test
    void historial_rechazaAdministrador() {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(usuarios.findById(ID_USUARIO)).thenReturn(Optional.of(usuario(RolUsuario.ADMINISTRADOR)));

        assertThrows(AccessDeniedException.class, () -> servicio.listarMovimientosActual(0, 20));
        verifyNoInteractions(acceso);
    }

    @Test
    void historialVacio_devuelvePaginaSinMovimientos() {
        prepararHistorial();
        PageRequest pagina = PageRequest.of(0, 20);
        when(fuente.consultarMovimientos(ID_USUARIO, pagina)).thenReturn(new PageImpl<>(List.of(), pagina, 0));

        RespuestaPaginada<RespuestaMovimientoPuntos> respuesta = servicio.listarMovimientosActual(0, 20);

        assertTrue(respuesta.contenido().isEmpty());
        assertEquals(0, respuesta.totalElementos());
    }

    private void prepararHistorial() {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(usuarios.findById(ID_USUARIO)).thenReturn(Optional.of(usuario(RolUsuario.PASAJERO)));
        when(acceso.exigir()).thenReturn(fuente);
    }

    private void prepararResumen() {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(acceso.exigir()).thenReturn(fuente);
        when(fuente.contarCanjes(Set.of(1L, 2L, 3L))).thenReturn(Map.of());
    }

    private static Usuario usuario(RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setId(ID_USUARIO);
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuario;
    }

    private static MovimientoPuntos movimiento(Long id, TipoMovimiento tipo, long cantidad, String descripcion,
                                               Long idBeneficio) {
        return new MovimientoPuntos(id, tipo, cantidad, FECHA, descripcion,
                tipo == TipoMovimiento.ACUMULACION ? "ABC123" : null, idBeneficio);
    }
}
