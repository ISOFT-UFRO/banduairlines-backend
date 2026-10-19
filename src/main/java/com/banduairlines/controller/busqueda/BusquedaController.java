package com.banduairlines.controller.busqueda;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import com.banduairlines.dto.busqueda.AeropuertoBusquedaResponse;
import com.banduairlines.dto.busqueda.AlternativaVueloResponse;
import com.banduairlines.dto.busqueda.BusquedaVueloRequest;
import com.banduairlines.dto.busqueda.OrdenVuelo;
import com.banduairlines.dto.busqueda.VueloBusquedaResponse;
import com.banduairlines.service.busqueda.BusquedaService;

@RestController
@RequestMapping("/api/v1/busqueda")
public class BusquedaController {
    private final BusquedaService busqueda;

    public BusquedaController(BusquedaService busqueda) {
        this.busqueda = busqueda;
    }

    @GetMapping("/vuelos/buscar")
    public List<VueloBusquedaResponse> buscarVuelos(@Valid @ModelAttribute BusquedaVueloRequest criterios) {
        return busqueda.buscarVuelos(criterios);
    }

    @GetMapping("/vuelos/comparar")
    public List<AlternativaVueloResponse> compararVuelos(@Valid @ModelAttribute BusquedaVueloRequest criterios,
            @RequestParam(defaultValue = "HORARIO") OrdenVuelo orden) {
        return busqueda.compararVuelos(criterios, orden);
    }

    @GetMapping("/aeropuertos")
    public List<AeropuertoBusquedaResponse> listarAeropuertos() {
        return busqueda.listarAeropuertos();
    }
}
