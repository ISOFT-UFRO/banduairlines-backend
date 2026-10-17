package com.banduairlines.controller.reserva;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.banduairlines.dto.reserva.ResultadoPagoRequest;
import com.banduairlines.dto.reserva.ResultadoPagoResponse;
import com.banduairlines.service.reserva.ClaveServicioInterna;
import com.banduairlines.service.reserva.ReservaResultadoPagoService;

// endpoint 13. Acceso: servicio interno (PAGOS) con la clave compartida en X-Service-Key. senecesita persimo para conf9ogurar 
//Application.properties y colocar app.internal.service-key=${SERVICE_KEY:} para que este endpoint funcione 
//La transacción con que se confirmó cada reserva se recuerda en TransaccionesPagoProvisional (sin columna nueva).
@RestController
@RequestMapping("/api/v1/internas/reservas/{reservaId}")
public class ReservaResultadoPagoController {

    private final ReservaResultadoPagoService reservaResultadoPagoService;
    private final ClaveServicioInterna claveServicioInterna;

    public ReservaResultadoPagoController(ReservaResultadoPagoService reservaResultadoPagoService,
                                          ClaveServicioInterna claveServicioInterna) {
        this.reservaResultadoPagoService = reservaResultadoPagoService;
        this.claveServicioInterna = claveServicioInterna;
    }

    @PostMapping("/resultado-pago")
    public ResultadoPagoResponse registrarResultadoPago(
            @PathVariable Long reservaId,
            @RequestHeader(value = "X-Service-Key", required = false) String claveServicio,
            @Valid @RequestBody ResultadoPagoRequest request) {
        claveServicioInterna.exigir(claveServicio);
        return reservaResultadoPagoService.registrar(reservaId, request);
    }
}
