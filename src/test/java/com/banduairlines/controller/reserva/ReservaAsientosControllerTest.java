package com.banduairlines.controller.reserva;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.banduairlines.dto.reserva.MapaAsientosResponse;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.service.reserva.ReservaAsientosService;

@ExtendWith(MockitoExtension.class)
class ReservaAsientosControllerTest {

    @Mock private ReservaAsientosService reservaAsientosService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReservaAsientosController(reservaAsientosService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void obtenerMapa_responde200ConFilasYAsientos() throws Exception {
        when(reservaAsientosService.obtenerMapa(42L, 10L, "tok")).thenReturn(new MapaAsientosResponse(10L, ClaseCabina.ECONOMY,
                List.of("A", "B", "C", "D", "E", "F"), List.of("C"),
                List.of(new MapaAsientosResponse.Fila(10, ClaseCabina.ECONOMY, List.of(
                        new MapaAsientosResponse.Asiento("10A", "A", "ESTANDAR", "DISPONIBLE", 0, true))))));

        mockMvc.perform(get("/api/v1/reservas/42/tramos/10/asientos").header("X-Compra-Token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasillosDespuesDe[0]").value("C"))
                .andExpect(jsonPath("$.filas[0].asientos[0].codigo").value("10A"))
                .andExpect(jsonPath("$.filas[0].asientos[0].estado").value("DISPONIBLE"));
    }

    @Test
    void obtenerMapa_tramoAjeno_responde404() throws Exception {
        when(reservaAsientosService.obtenerMapa(42L, 99L, null)).thenThrow(new RecursoNoEncontradoException("Tramo", 99L));

        mockMvc.perform(get("/api/v1/reservas/42/tramos/99/asientos"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NO_ENCONTRADO"));
    }
}
