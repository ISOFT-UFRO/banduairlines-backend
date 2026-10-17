package com.banduairlines.controller.tracking;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.tracking.VueloPasajeroDTO;
import com.banduairlines.service.tracking.TrackingPasajeroService;

import jakarta.validation.constraints.NotBlank;

// RF-04: pasajeros consultan el estado de sus vuelos.
@RestController
@RequestMapping("/api/v1/tracking")
@Validated
public class TrackingPasajeroController {

    private final TrackingPasajeroService trackingPasajeroService;

    public TrackingPasajeroController(TrackingPasajeroService trackingPasajeroService) {
        this.trackingPasajeroService = trackingPasajeroService;
    }

    // Pasajero autenticado (JWT).
    @GetMapping("/vuelos")
    public List<VueloPasajeroDTO> consultarMisVuelos() {
        return trackingPasajeroService.consultarMisVuelos();
    }

    // Visitante: mecanismo de consulta válido sin cuenta.
    @GetMapping("/vuelos/consulta")
    public List<VueloPasajeroDTO> consultarVuelosDeReserva(
            @RequestParam @NotBlank(message = "el código de reserva es obligatorio") String codigoReserva,
            @RequestParam @NotBlank(message = "el número de documento es obligatorio") String numeroDocumento) {
        return trackingPasajeroService.consultarVuelosDeReserva(codigoReserva, numeroDocumento);
    }
}
