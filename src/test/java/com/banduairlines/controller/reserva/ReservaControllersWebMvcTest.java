package com.banduairlines.controller.reserva;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.dto.reserva.ResultadoPagoResponse;
import com.banduairlines.exception.DemasiadosIntentosException;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.service.reserva.ClaveServicioInterna;
import com.banduairlines.service.reserva.ReservaCuentaService;
import com.banduairlines.service.reserva.ReservaHistorialService;
import com.banduairlines.service.reserva.ReservaResultadoPagoService;

/** Controllers de los endpoints 13 a 16 (MockMvc standalone, como el resto de tests de controllers de RESERVA). */
@ExtendWith(MockitoExtension.class)
class ReservaControllersWebMvcTest {

    private static final String PAGADO = """
            {"pagoId":"P1","transaccionId":"TX1","estado":"PAGADO","montoCLP":50000,"fecha":"2026-10-17T10:00:00-03:00"}
            """;

    @Mock private ReservaResultadoPagoService resultadoPagoService;
    @Mock private ReservaHistorialService historialService;
    @Mock private ReservaCuentaService cuentaService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new ReservaResultadoPagoController(resultadoPagoService, new ClaveServicioInterna("secret")),
                        new ReservaHistorialController(historialService),
                        new ReservaCuentaController(cuentaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void postResultadoPago_conLaClave_responde200() throws Exception {
        when(resultadoPagoService.registrar(eq(1L), any()))
                .thenReturn(new ResultadoPagoResponse(1L, EstadoReserva.CONFIRMADA));

        mockMvc.perform(post("/api/v1/internas/reservas/1/resultado-pago")
                        .header("X-Service-Key", "secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAGADO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservaId").value(1))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    void postResultadoPago_sinClave_responde401SinRegistrar() throws Exception {
        mockMvc.perform(post("/api/v1/internas/reservas/1/resultado-pago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAGADO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
        verify(resultadoPagoService, never()).registrar(any(), any());
    }

    @Test
    void putAsociarCuenta_responde200ConLaReserva() throws Exception {
        when(cuentaService.asociar(1L, "token-invitado")).thenReturn(new ReservaResponse(1L, "ABC123",
                EstadoReserva.CONFIRMADA, null, "Light", new ReservaResponse.CantidadPasajeros(1, 0, 0), List.of(),
                true, List.of(), List.of(), List.of(), null, null, null));

        mockMvc.perform(put("/api/v1/reservas/1/cuenta").header("X-Compra-Token", "token-invitado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoReserva").value("ABC123"));
    }

    @Test
    void getBuscar_sinCupo_responde429ConRetryAfter() throws Exception {
        when(historialService.buscar(eq("ABC123"), eq("tomas@correo.cl"), anyString()))
                .thenThrow(new DemasiadosIntentosException(600));

        mockMvc.perform(get("/api/v1/reservas/buscar").param("codigo", "ABC123").param("correo", "tomas@correo.cl"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "600"))
                .andExpect(jsonPath("$.code").value("DEMASIADOS_INTENTOS"));
    }
}
