package com.banduairlines.controller.tracking;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.banduairlines.service.tracking.TrackingTableroService;

@WebMvcTest(controllers = TrackingTableroController.class, properties = "app.security.enabled=true")
@Import({SecurityConfig.class, MethodSecurityConfig.class, JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class TrackingTableroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackingTableroService tableroService;

    @Test
    void tableroDelDia_conAdmin_devuelve200ConLista() throws Exception {
        LocalDateTime salida = LocalDateTime.of(2026, 10, 16, 10, 0);
        when(tableroService.tableroDelDia(null, null)).thenReturn(List.of(
                new ResumenVueloDTO(1L, 100L, "BD001", "SCL", "PMC", salida,
                        EstadoVuelo.PROGRAMADO, 0, salida.minusMinutes(5))
        ));

        mockMvc.perform(get("/api/v1/admin/tracking/tablero").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idSeguimiento").value(1))
                .andExpect(jsonPath("$[0].vueloId").value(100))
                .andExpect(jsonPath("$[0].codigoVuelo").value("BD001"))
                .andExpect(jsonPath("$[0].origenIata").value("SCL"))
                .andExpect(jsonPath("$[0].destinoIata").value("PMC"))
                .andExpect(jsonPath("$[0].estadoActual").value("PROGRAMADO"))
                .andExpect(jsonPath("$[0].retrasoMinutos").value(0));
    }

    @Test
    void tableroDelDia_conFiltroEstado_devuelve200() throws Exception {
        LocalDateTime salida = LocalDateTime.of(2026, 10, 16, 14, 0);
        when(tableroService.tableroDelDia(null, EstadoVuelo.EN_VUELO)).thenReturn(List.of(
                new ResumenVueloDTO(2L, 101L, "BD002", "SCL", "CJC", salida,
                        EstadoVuelo.EN_VUELO, 0, salida.minusMinutes(2))
        ));

        mockMvc.perform(get("/api/v1/admin/tracking/tablero?estado=EN_VUELO")
                .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoVuelo").value("BD002"))
                .andExpect(jsonPath("$[0].estadoActual").value("EN_VUELO"));
    }

    @Test
    void tableroDelDia_conFecha_propagaFechaAlServicio() throws Exception {
        when(tableroService.tableroDelDia(LocalDate.of(2026, 10, 20), null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/tracking/tablero?fecha=2026-10-20")
                .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void tableroDelDia_conFechaInvalida_devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tracking/tablero?fecha=20-10-2026")
                .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PETICION_MALFORMADA"));
        verifyNoInteractions(tableroService);
    }

    @Test
    void tableroDelDia_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tracking/tablero"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
        verifyNoInteractions(tableroService);
    }

    @Test
    void tableroDelDia_conRolPasajero_devuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tracking/tablero").with(user("user").roles("PASAJERO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
        verifyNoInteractions(tableroService);
    }

    @Test
    void tableroDelDia_conEstadoInvalido_devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tracking/tablero?estado=DESPEGANDO_EXTRANO")
                .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PETICION_MALFORMADA"));
        verifyNoInteractions(tableroService);
    }
}