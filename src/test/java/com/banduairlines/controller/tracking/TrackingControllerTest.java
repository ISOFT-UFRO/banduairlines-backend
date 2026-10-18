package com.banduairlines.controller.tracking;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.service.impl.JwtService;
import com.banduairlines.dto.tracking.EventoEstadoDTO;
import com.banduairlines.dto.tracking.SeguimientoVueloDTO;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.OrigenEvento;
import com.banduairlines.security.JwtAuthenticationFilter;
import com.banduairlines.security.MethodSecurityConfig;
import com.banduairlines.security.RestAccessDeniedHandler;
import com.banduairlines.security.RestAuthenticationEntryPoint;
import com.banduairlines.security.SecurityConfig;
import com.banduairlines.service.tracking.CodigosTracking;
import com.banduairlines.service.tracking.TrackingService;

@WebMvcTest(controllers = TrackingController.class, properties = "app.security.enabled=true")
@Import({SecurityConfig.class, MethodSecurityConfig.class, JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class TrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Dependencias del filtro JWT de Cuentas; los tests autentican con spring-security-test.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private TrackingService trackingService;

    @ParameterizedTest
    @ValueSource(strings = {"PASAJERO", "ADMINISTRADOR"})
    void obtenerPorId_conRolPermitido_devuelve200ConContrato(String rol) throws Exception {
        LocalDateTime salida = LocalDateTime.of(2026, 10, 16, 10, 0);
        EventoEstadoDTO evento = new EventoEstadoDTO(12L, EstadoVuelo.EMBARCANDO, EstadoVuelo.EN_VUELO,
                salida, OrigenEvento.SISTEMA, null);
        when(trackingService.obtenerPorId(42L)).thenReturn(new SeguimientoVueloDTO(
                7L, 42L, "BD0482", "SCL", "ZCO", salida, salida.plusHours(1),
                EstadoVuelo.EN_VUELO, 0, List.of(evento)));

        mockMvc.perform(get("/api/v1/tracking/vuelos/42").with(user("usuario").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSeguimiento").value(7))
                .andExpect(jsonPath("$.vueloId").value(42))
                .andExpect(jsonPath("$.codigoVuelo").value("BD0482"))
                .andExpect(jsonPath("$.origenIata").value("SCL"))
                .andExpect(jsonPath("$.destinoIata").value("ZCO"))
                .andExpect(jsonPath("$.salidaProgramada").value("2026-10-16T10:00:00"))
                .andExpect(jsonPath("$.estadoActual").value("EN_VUELO"))
                .andExpect(jsonPath("$.retrasoMinutos").value(0))
                .andExpect(jsonPath("$.historial[0].idEvento").value(12))
                .andExpect(jsonPath("$.historial[0].origen").value("SISTEMA"))
                .andExpect(jsonPath("$.historial[0].actualizadoPor").doesNotExist());
    }

    @Test
    void obtenerPorId_sinSeguimiento_devuelve404Centralizado() throws Exception {
        when(trackingService.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("Sin seguimiento")
                .conCodigo(CodigosTracking.VUELO_NO_ENCONTRADO));

        mockMvc.perform(get("/api/v1/tracking/vuelos/99").with(user("pasajero").roles("PASAJERO")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VUELO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/tracking/vuelos/99"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "9223372036854775808"})
    void obtenerPorId_identificadorMalformado_devuelve400(String id) throws Exception {
        mockMvc.perform(get("/api/v1/tracking/vuelos/" + id).with(user("pasajero").roles("PASAJERO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PETICION_MALFORMADA"));
        verifyNoInteractions(trackingService);
    }

    @Test
    void obtenerPorId_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/vuelos/42"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
        verifyNoInteractions(trackingService);
    }

    @Test
    void obtenerPorId_conRolNoPermitido_devuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/vuelos/42").with(user("otro").roles("OTRO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
        verifyNoInteractions(trackingService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PASAJERO", "ADMINISTRADOR"})
    void obtenerPorCodigo_conRolPermitido_devuelve200ConHistorial(String rol) throws Exception {
        LocalDateTime salida = LocalDateTime.of(2026, 10, 16, 10, 0);
        EventoEstadoDTO evento = new EventoEstadoDTO(12L, EstadoVuelo.EMBARCANDO, EstadoVuelo.EN_VUELO,
                salida, OrigenEvento.SISTEMA, null);
        when(trackingService.obtenerPorCodigo("BD0482")).thenReturn(new SeguimientoVueloDTO(
                7L, 42L, "BD0482", "SCL", "ZCO", salida, salida.plusHours(1),
                EstadoVuelo.EN_VUELO, 0, List.of(evento)));

        mockMvc.perform(get("/api/v1/tracking/vuelos/codigo/BD0482").with(user("usuario").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSeguimiento").value(7))
                .andExpect(jsonPath("$.vueloId").value(42))
                .andExpect(jsonPath("$.codigoVuelo").value("BD0482"))
                .andExpect(jsonPath("$.origenIata").value("SCL"))
                .andExpect(jsonPath("$.destinoIata").value("ZCO"))
                .andExpect(jsonPath("$.salidaProgramada").value("2026-10-16T10:00:00"))
                .andExpect(jsonPath("$.llegadaProgramada").value("2026-10-16T11:00:00"))
                .andExpect(jsonPath("$.estadoActual").value("EN_VUELO"))
                .andExpect(jsonPath("$.retrasoMinutos").value(0))
                .andExpect(jsonPath("$.historial.length()").value(1))
                .andExpect(jsonPath("$.historial[0].idEvento").value(12))
                .andExpect(jsonPath("$.historial[0].estadoAnterior").value("EMBARCANDO"))
                .andExpect(jsonPath("$.historial[0].estadoNuevo").value("EN_VUELO"))
                .andExpect(jsonPath("$.historial[0].fechaCambio").value("2026-10-16T10:00:00"))
                .andExpect(jsonPath("$.historial[0].origen").value("SISTEMA"))
                .andExpect(jsonPath("$.historial[0].actualizadoPor").doesNotExist());
    }

    @Test
    void obtenerPorCodigo_sinSeguimiento_devuelve404Centralizado() throws Exception {
        when(trackingService.obtenerPorCodigo("BD9999")).thenThrow(new RecursoNoEncontradoException("Sin seguimiento")
                .conCodigo(CodigosTracking.VUELO_NO_ENCONTRADO));

        mockMvc.perform(get("/api/v1/tracking/vuelos/codigo/BD9999").with(user("pasajero").roles("PASAJERO")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VUELO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/tracking/vuelos/codigo/BD9999"));
    }

    @Test
    void obtenerPorCodigo_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/vuelos/codigo/BD0482"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
        verifyNoInteractions(trackingService);
    }

    @Test
    void obtenerPorCodigo_conRolNoPermitido_devuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/vuelos/codigo/BD0482").with(user("otro").roles("OTRO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
        verifyNoInteractions(trackingService);
    }
}
