package com.banduairlines.controller.reserva;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.service.reserva.ReservaCuentaService;

//endpoint 16 pasajero con JWT, más el X-Compra-Token de esa reserva
@RestController
@RequestMapping("/api/v1/reservas/{reservaId}")
public class ReservaCuentaController {

    private final ReservaCuentaService reservaCuentaService;

    public ReservaCuentaController(ReservaCuentaService reservaCuentaService) {
        this.reservaCuentaService = reservaCuentaService;
    }

    @PutMapping("/cuenta")
    public ReservaResponse asociarCuenta(
            @PathVariable Long reservaId,
            @RequestHeader(value = "X-Compra-Token", required = false) String compraToken) {
        return reservaCuentaService.asociar(reservaId, compraToken);
    }
}