package com.banduairlines.service;

import com.banduairlines.dto.cuenta.ActualizacionFidelizacion;
import com.banduairlines.dto.cuenta.ConfiguracionNivel;
import com.banduairlines.dto.cuenta.RespuestaFidelizacion;
import com.banduairlines.dto.cuenta.RespuestaMovimientoPuntos;
import com.banduairlines.dto.cuenta.RespuestaPaginada;
import com.banduairlines.exception.ServicioExternoException;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.model.Beneficio;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.persistence.cuenta.RegistroPuntos;
import com.banduairlines.persistence.cuenta.TipoRegistroPuntos;
import com.banduairlines.repository.BeneficioRepository;
import com.banduairlines.repository.CuentaPuntosRepository;
import com.banduairlines.repository.HistorialPuntosRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.cuenta.ActualizacionFidelizacionService;
import com.banduairlines.service.cuenta.CodigosFidelizacion;
import com.banduairlines.service.cuenta.EvaluadorBeneficio;
import com.banduairlines.service.cuenta.GestionNivelService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    private BeneficioRepository beneficios;
    @Mock
    private HistorialPuntosRepository historial;
    @Mock
    private CuentaPuntosRepository cuentas;
    @Mock
    private UsuarioActual usuarioActual;

    private FidelizacionServiceImpl servicio;

    @BeforeEach
    void setUp() {
        MapeadorFidelizacion mapeador = new MapeadorFidelizacion(beneficios, new EvaluadorBeneficio());
        servicio = new FidelizacionServiceImpl(actualizacion, niveles, beneficios, historial, cuentas,
                usuarioActual, mapeador);
    }

    @Test
    void consultaFidelizacion_armaResumenConProgresoBeneficiosYMovimientos() {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(actualizacion.actualizarActual()).thenReturn(new ActualizacionFidelizacion(
                new BigDecimal("12500.500000"), 900, Nivel.PLATA, new BigDecimal("1.2")));
        when(niveles.obtenerConfiguracion(Nivel.PLATA))
                .thenReturn(new ConfiguracionNivel(Nivel.PLATA, 10000, new BigDecimal("1.2")));
        when(niveles.obtenerConfiguracion(Nivel.ORO))
                .thenReturn(new ConfiguracionNivel(Nivel.ORO, 25000, new BigDecimal("1.5")));
        when(beneficios.findAllByOrderByCostoPuntosAscNombreAsc()).thenReturn(List.of(
                beneficio(1L, "Equipaje extra", Nivel.BASICO, 100, true),
                beneficio(2L, "Sala VIP", Nivel.ORO, 100, true),
                beneficio(3L, "Upgrade", Nivel.PLATA, 5000, true)));
        when(historial.findTop5ByIdUsuarioOrderByFechaDescIdRegistroDesc(ID_USUARIO)).thenReturn(List.of(
                registro(40L, TipoRegistroPuntos.ACUMULACION, 300, "VUELO:4", "ABC123")));

        RespuestaFidelizacion respuesta = servicio.consultarActual();

        assertEquals(900, respuesta.puntosDisponibles());
        assertEquals(Nivel.PLATA, respuesta.nivelActual());
        assertEquals(new BigDecimal("1.2"), respuesta.multiplicadorPuntos());
        assertEquals(Nivel.ORO, respuesta.progreso().nivelSiguiente());
        assertEquals(25000L, respuesta.progreso().millasNivelSiguiente());
        assertEquals(12500, respuesta.progreso().millasFaltantes());
        assertEquals(16, respuesta.progreso().porcentaje());
        assertEquals(1, respuesta.beneficiosDisponibles().size());
        assertEquals(1L, respuesta.beneficiosDisponibles().get(0).idBeneficio());
        assertEquals(1, respuesta.movimientosRecientes().size());
        assertEquals("Acumulación por vuelo 4", respuesta.movimientosRecientes().get(0).descripcion());
        assertEquals("ABC123", respuesta.movimientosRecientes().get(0).codigoPNR());
        assertEquals(300, respuesta.movimientosRecientes().get(0).cantidadPuntos());
    }

    @Test
    void enNivelOro_elProgresoEstaCompleto() {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(actualizacion.actualizarActual()).thenReturn(new ActualizacionFidelizacion(
                new BigDecimal("40000"), 0, Nivel.ORO, new BigDecimal("1.5")));
        when(beneficios.findAllByOrderByCostoPuntosAscNombreAsc()).thenReturn(List.of());
        when(historial.findTop5ByIdUsuarioOrderByFechaDescIdRegistroDesc(ID_USUARIO)).thenReturn(List.of());

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
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(cuentas.findById(ID_USUARIO)).thenReturn(Optional.of(usuario(RolUsuario.PASAJERO)));
        PageRequest pagina = PageRequest.of(1, 2);
        List<RegistroPuntos> contenido = List.of(
                registro(51L, TipoRegistroPuntos.CANJE, 200, "CANJE:clave-prueba-1:B3", null),
                registro(40L, TipoRegistroPuntos.ACUMULACION, 300, "VUELO:4", "ABC123"));
        when(historial.findByIdUsuarioOrderByFechaDescIdRegistroDesc(ID_USUARIO, pagina))
                .thenReturn(new PageImpl<>(contenido, pagina, 5));
        when(beneficios.findAllById(any())).thenReturn(List.of(beneficio(3L, "Sala VIP", Nivel.BASICO, 200, true)));

        RespuestaPaginada<RespuestaMovimientoPuntos> respuesta = servicio.listarMovimientosActual(1, 2);

        assertEquals(1, respuesta.pagina());
        assertEquals(2, respuesta.tamano());
        assertEquals(5, respuesta.totalElementos());
        assertEquals(3, respuesta.totalPaginas());
        RespuestaMovimientoPuntos canje = respuesta.contenido().get(0);
        assertEquals(51L, canje.idMovimiento());
        assertEquals(TipoRegistroPuntos.CANJE, canje.tipo());
        assertEquals(3L, canje.idBeneficio());
        assertEquals("Canje de Sala VIP", canje.descripcion());
        assertEquals(-200, canje.cantidadPuntos());
        RespuestaMovimientoPuntos acumulacion = respuesta.contenido().get(1);
        assertNull(acumulacion.idBeneficio());
        assertEquals(300, acumulacion.cantidadPuntos());
    }

    @Test
    void historial_rechazaTamanoFueraDeRango() {
        assertThrows(ValidacionException.class, () -> servicio.listarMovimientosActual(0, 0));
        assertThrows(ValidacionException.class, () -> servicio.listarMovimientosActual(0, 101));
        assertThrows(ValidacionException.class, () -> servicio.listarMovimientosActual(-1, 20));
        verifyNoInteractions(historial);
    }

    @Test
    void historial_rechazaAdministrador() {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(cuentas.findById(ID_USUARIO)).thenReturn(Optional.of(usuario(RolUsuario.ADMINISTRADOR)));

        assertThrows(AccessDeniedException.class, () -> servicio.listarMovimientosActual(0, 20));
        verifyNoInteractions(historial);
    }

    @Test
    void historialVacio_devuelvePaginaSinMovimientos() {
        when(usuarioActual.exigirId()).thenReturn(ID_USUARIO);
        when(cuentas.findById(ID_USUARIO)).thenReturn(Optional.of(usuario(RolUsuario.PASAJERO)));
        PageRequest pagina = PageRequest.of(0, 20);
        when(historial.findByIdUsuarioOrderByFechaDescIdRegistroDesc(ID_USUARIO, pagina))
                .thenReturn(new PageImpl<>(List.of(), pagina, 0));

        RespuestaPaginada<RespuestaMovimientoPuntos> respuesta = servicio.listarMovimientosActual(0, 20);

        assertTrue(respuesta.contenido().isEmpty());
        assertEquals(0, respuesta.totalElementos());
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

    private static Beneficio beneficio(Long id, String nombre, Nivel nivel, int costo, boolean disponible) {
        return Beneficio.builder()
                .idBeneficio(id)
                .nombre(nombre)
                .nivelRequerido(nivel)
                .costoPuntos(costo)
                .disponible(disponible)
                .build();
    }

    private static RegistroPuntos registro(Long id, TipoRegistroPuntos tipo, long cantidad,
                                           String referencia, String codigoPNR) {
        RegistroPuntos registro = new RegistroPuntos(ID_USUARIO, tipo, cantidad, FECHA, referencia, codigoPNR);
        ReflectionTestUtils.setField(registro, "idRegistro", id);
        return registro;
    }
}
