package com.banduairlines.controller.tracking;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.tracking.ResumenVueloDTO;
import com.banduairlines.service.tracking.TrackingPasajeroService;

// RF-04: el usuario no registrado consulta su vuelo en Check-In (acuerdo del 16-10), no aquí.
@RestController
@RequestMapping("/api/v1/tracking")
public class TrackingPasajeroController {

    private final TrackingPasajeroService trackingPasajeroService;

    public TrackingPasajeroController(TrackingPasajeroService trackingPasajeroService) {
        this.trackingPasajeroService = trackingPasajeroService;
    }

    @GetMapping("/mis-vuelos")
    @PreAuthorize("hasRole('PASAJERO')")
    public List<ResumenVueloDTO> consultarMisVuelos() {
        return trackingPasajeroService.consultarMisVuelos();
    }
}
