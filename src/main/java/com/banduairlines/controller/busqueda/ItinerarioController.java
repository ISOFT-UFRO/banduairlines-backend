package com.banduairlines.controller.busqueda;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.busqueda.BusquedaItinerarioRequest;
import com.banduairlines.dto.busqueda.BusquedaItinerarioResponse;
import com.banduairlines.service.busqueda.ItinerarioBusquedaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/busqueda")
@RequiredArgsConstructor
public class ItinerarioController {

    private final ItinerarioBusquedaService itinerarioBusquedaService;

    @GetMapping("/itinerario")
    public BusquedaItinerarioResponse buscar(@Valid @ModelAttribute BusquedaItinerarioRequest request) {
        return itinerarioBusquedaService.buscar(request);
    }
}
