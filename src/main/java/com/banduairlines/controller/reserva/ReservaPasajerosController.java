package com.banduairlines.controller.reserva;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.reserva.ContactoRequest;
import com.banduairlines.dto.reserva.ContactoResponse;
import com.banduairlines.dto.reserva.PasajerosRegistradosResponse;
import com.banduairlines.dto.reserva.RegistrarPasajerosRequest;
import com.banduairlines.service.reserva.ReservaContactoService;
import com.banduairlines.service.reserva.ReservaPasajerosService;

import jakarta.validation.Valid;

/** RESERVA, endpoints 4 y 5: datos de los pasajeros y contacto de la compra */
@RestController
@RequestMapping("/api/v1/reservas/{reservaId}")
public class ReservaPasajerosController {

    private final ReservaPasajerosService reservaPasajerosService;
    private final ReservaContactoService reservaContactoService;

    public ReservaPasajerosController(ReservaPasajerosService reservaPasajerosService,
                                      ReservaContactoService reservaContactoService) {
        this.reservaPasajerosService = reservaPasajerosService;
        this.reservaContactoService = reservaContactoService;
    }

    @PutMapping("/pasajeros")
    public PasajerosRegistradosResponse registrarPasajeros(
            @PathVariable Long reservaId,
            @RequestHeader(value = ReservaController.COMPRA_TOKEN, required = false) String compraToken,
            @Valid @RequestBody RegistrarPasajerosRequest request) {
        return reservaPasajerosService.registrar(reservaId, compraToken, request);
    }

    @PutMapping("/contacto")
    public ContactoResponse registrarContacto(
            @PathVariable Long reservaId,
            @RequestHeader(value = ReservaController.COMPRA_TOKEN, required = false) String compraToken,
            @Valid @RequestBody ContactoRequest request) {
        return reservaContactoService.registrar(reservaId, compraToken, request);
    }
}
