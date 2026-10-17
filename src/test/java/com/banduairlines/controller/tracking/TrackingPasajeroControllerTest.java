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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.banduairlines.dto.tracking.ResumenVueloDTO;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.security.JwtAuthenticationFilter;
import com.banduairlines.security.MethodSecurityConfig;
import com.banduairlines.security.RestAccessDeniedHandler;
import com.banduairlines.security.RestAuthenticationEntryPoint;
import com.banduairlines.security.SecurityConfig;
import com.banduairlines.service.tracking.TrackingPasajeroService;

@WebMvcTest(controllers = TrackingPasajeroController.class, properties = "app.security.enabled=true")
@Import({SecurityConfig.class, MethodSecurityConfig.class, JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class TrackingPasajeroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackingPasajeroService trackingPasajeroService;

    @Test
    void consultarMisVuelos_conPasajero_devuelve200ConContrato() throws Exception {
        LocalDateTime salida = LocalDateTime.of(2026, 10, 16, 10, 0);
        when(trackingPasajeroService.consultarMisVuelos()).thenReturn(List.of(
                new ResumenVueloDTO(7L, 42L, "BD0482", "SCL", "ZCO", salida,
                        EstadoVuelo.EN_VUELO, 0, salida.plusMinutes(5))));

        mockMvc.perform(get("/api/v1/tracking/mis-vuelos").with(user("pasajero").roles("PASAJERO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idSeguimiento").value(7))
                .andExpect(jsonPath("$[0].vueloId").value(42))
                .andExpect(jsonPath("$[0].codigoVuelo").value("BD0482"))
                .andExpect(jsonPath("$[0].origenIata").value("SCL"))
                .andExpect(jsonPath("$[0].destinoIata").value("ZCO"))
                .andExpect(jsonPath("$[0].salidaProgramada").value("2026-10-16T10:00:00"))
                .andExpect(jsonPath("$[0].estadoActual").value("EN_VUELO"))
                .andExpect(jsonPath("$[0].retrasoMinutos").value(0));
    }

    @Test
    void consultarMisVuelos_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/mis-vuelos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
        verifyNoInteractions(trackingPasajeroService);
    }

    @Test
    void consultarMisVuelos_conAdministrador_devuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/mis-vuelos").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
        verifyNoInteractions(trackingPasajeroService);
    }
}
