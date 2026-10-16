package com.banduairlines.controller.busqueda;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.busqueda.PasajerosBusquedaRequest;
import com.banduairlines.dto.busqueda.TarifasBusquedaRequest;
import com.banduairlines.dto.busqueda.TarifasBusquedaResponse;
import com.banduairlines.service.busqueda.TarifasBusquedaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/busqueda")
@Validated
@RequiredArgsConstructor
@Tag(name = "Búsqueda y Disponibilidad")
public class TarifasBusquedaController {

    private final TarifasBusquedaService tarifasBusquedaService;

    @GetMapping("/tarifas")
    @Operation(summary = "Consultar packs y precios de un itinerario (RF-05)",
            description = "Recibe uno o dos vueloIds en orden. Devuelve precios del catálogo por pasajero y "
                    + "la suma de tarifas para el grupo; no es el total final de compra con extras o promociones.")
    public TarifasBusquedaResponse consultar(@Valid @ModelAttribute TarifasBusquedaRequest request) {
        return tarifasBusquedaService.consultar(request);
    }

    @GetMapping("/vuelos/{idVuelo}/tarifas")
    @Operation(summary = "Consultar packs y precios de un vuelo (RF-05)")
    public TarifasBusquedaResponse consultarVuelo(
            @PathVariable @Positive Long idVuelo, @Valid @ModelAttribute PasajerosBusquedaRequest pasajeros) {
        TarifasBusquedaRequest request = new TarifasBusquedaRequest();
        request.setVueloIds(List.of(idVuelo));
        request.setCantidadAdultos(pasajeros.getCantidadAdultos());
        request.setCantidadNinos(pasajeros.getCantidadNinos());
        request.setCantidadBebes(pasajeros.getCantidadBebes());
        return tarifasBusquedaService.consultar(request);
    }
}
