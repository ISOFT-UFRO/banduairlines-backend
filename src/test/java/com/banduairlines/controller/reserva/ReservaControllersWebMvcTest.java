package com.banduairlines.controller.reserva;

import com.banduairlines.dto.reserva.ResultadoPagoRequest;
import com.banduairlines.dto.reserva.ResultadoPagoResponse;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.service.reserva.ReservaConsultaService;
import com.banduairlines.service.reserva.ReservaCuentaService;
import com.banduairlines.service.reserva.ReservaHistorialService;
import com.banduairlines.service.reserva.ReservaResultadoPagoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = {
        ReservaResultadoPagoController.class,
        ReservaHistorialController.class,
        ReservaCuentaController.class,
        GlobalExceptionHandler.class
})
@AutoConfigureMockMvc(addFilters = false)
class ReservaControllersWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservaResultadoPagoService resultadoPagoService;

    @MockBean
    private ReservaConsultaService consultaService;

    @MockBean
    private ReservaHistorialService historialService;

    @MockBean
    private ReservaCuentaService cuentaService;

    @Test
    void postResultadoPago_DebeRetornar200OK() throws Exception {
        ResultadoPagoRequest req = new ResultadoPagoRequest(
                "P1", "TX1", "PAGADO", 50000L, LocalDateTime.now(), null
        );

        when(resultadoPagoService.registrarResultadoPago(eq(1L), any(), eq("secret")))
                .thenReturn(new ResultadoPagoResponse(1L, EstadoReserva.CONFIRMADA));

        mockMvc.perform(post("/api/v1/internas/reservas/1/resultado-pago")
                        .header("X-Service-Key", "secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservaId").value(1))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    void putAsociarCuenta_DebeRetornar200OK() throws Exception {
        mockMvc.perform(put("/api/v1/reservas/1/cuenta")
                        .header("X-Compra-Token", "token-invitado"))
                .andExpect(status().isOk());
    }
}
