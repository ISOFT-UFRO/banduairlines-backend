package com.banduairlines.controller.busqueda;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.banduairlines.dto.busqueda.AeropuertoBusquedaResponse;
import com.banduairlines.dto.busqueda.BusquedaVueloRequest;
import com.banduairlines.dto.busqueda.VueloBusquedaResponse;
import com.banduairlines.service.BusquedaService;

@RestController
@RequestMapping("/api/v1/busqueda")
public class BusquedaController {
    private final BusquedaService busqueda;

    public BusquedaController(BusquedaService busqueda) {
        this.busqueda = busqueda;
    }

    @GetMapping("/vuelos")
    public List<VueloBusquedaResponse> buscarVuelos(@Valid @ModelAttribute BusquedaVueloRequest criterios) {
        return busqueda.buscarVuelos(criterios);
    }

    @GetMapping("/aeropuertos")
    public List<AeropuertoBusquedaResponse> listarAeropuertos() {
        return busqueda.listarAeropuertos();
    }
}
