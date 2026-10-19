package com.banduairlines.controller.reserva;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.reserva.CrearReservaRequest;
import com.banduairlines.dto.reserva.ReservaCreadaResponse;
import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.service.reserva.ReservaConsultaService;
import com.banduairlines.service.reserva.ReservaCreacionService;

import jakarta.validation.Valid;

/**
 * RESERVA, endpoints 1 y 2.
 * Crear: público (invitado) o PASAJERO con JWT. Consultar: JWT del dueño o X-Compra-Token del invitado.
 * El endpoint 3 (POST /{reservaId}/cancelacion) está en ReservaPagoController, junto al endpoint 12 que es la misma ruta
 */
@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    public static final String COMPRA_TOKEN = "X-Compra-Token";

    private final ReservaCreacionService reservaCreacionService;
    private final ReservaConsultaService reservaConsultaService;

    public ReservaController(ReservaCreacionService reservaCreacionService,
                             ReservaConsultaService reservaConsultaService) {
        this.reservaCreacionService = reservaCreacionService;
        this.reservaConsultaService = reservaConsultaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaCreadaResponse crearReserva(@Valid @RequestBody CrearReservaRequest request) {
        return reservaCreacionService.crear(request);
    }

    @GetMapping("/{reservaId}")
    public ReservaResponse obtenerReserva(
            @PathVariable Long reservaId,
            @RequestHeader(value = COMPRA_TOKEN, required = false) String compraToken) {
        return reservaConsultaService.obtener(reservaId, compraToken);
    }
}
