package com.banduairlines.controller.reserva;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.reserva.MapaAsientosResponse;
import com.banduairlines.service.reserva.ReservaAsientosService;

/** RESERVA, endpoint 6: mapa de asientos de un tramo de la reserva */
@RestController
@RequestMapping("/api/v1/reservas/{reservaId}/tramos/{tramoId}")
public class ReservaAsientosController {

    private final ReservaAsientosService reservaAsientosService;

    public ReservaAsientosController(ReservaAsientosService reservaAsientosService) {
        this.reservaAsientosService = reservaAsientosService;
    }

    @GetMapping("/asientos")
    public MapaAsientosResponse obtenerMapaAsientos(
            @PathVariable Long reservaId,
            @PathVariable Long tramoId,
            @RequestHeader(value = ReservaController.COMPRA_TOKEN, required = false) String compraToken) {
        return reservaAsientosService.obtenerMapa(reservaId, tramoId, compraToken);
    }
}
