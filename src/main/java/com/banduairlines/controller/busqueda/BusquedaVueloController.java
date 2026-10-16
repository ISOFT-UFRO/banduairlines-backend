package com.banduairlines.controller.busqueda;

import com.banduairlines.dto.busqueda.ClaseCabinaDisponibilidadResponse;
import com.banduairlines.dto.busqueda.ItinerarioVueloResponse;
import com.banduairlines.dto.busqueda.BusquedaItinerarioRequest;
import com.banduairlines.dto.busqueda.PasajerosBusquedaRequest;
import com.banduairlines.service.busqueda.BusquedaVueloService;
import com.banduairlines.service.busqueda.ItinerarioBusquedaService;
import com.banduairlines.service.busqueda.PasajerosBusquedaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/busqueda")
@Validated
@Tag(name = "Búsqueda y Disponibilidad", description = "Endpoints para buscar alternativas de vuelos y disponibilidad de clases de cabina")
public class BusquedaVueloController {

    private final BusquedaVueloService busquedaVueloService;
    private final ItinerarioBusquedaService itinerarioBusquedaService;
    private final PasajerosBusquedaService pasajerosBusquedaService;

    public BusquedaVueloController(BusquedaVueloService busquedaVueloService,
                                  ItinerarioBusquedaService itinerarioBusquedaService,
                                  PasajerosBusquedaService pasajerosBusquedaService) {
        this.busquedaVueloService = busquedaVueloService;
        this.itinerarioBusquedaService = itinerarioBusquedaService;
        this.pasajerosBusquedaService = pasajerosBusquedaService;
    }

    @GetMapping("/itinerarios")
    @Operation(
            summary = "Buscar alternativas de vuelo (RF-03)",
            description = "Permite visualizar alternativas de vuelo directas y con una conexión (escala), indicando horarios y tramos involucrados."
    )
    public ResponseEntity<List<ItinerarioVueloResponse>> buscarItinerarios(
            @Valid @ModelAttribute BusquedaItinerarioRequest request
    ) {
        return ResponseEntity.ok(itinerarioBusquedaService.buscar(request).itinerarios());
    }

    @GetMapping("/vuelos/{idVuelo}/disponibilidad")
    @Operation(
            summary = "Obtener clases de cabina disponibles por vuelo (RF-04)",
            description = "Permite visualizar, para una alternativa de vuelo específica, las clases de cabina disponibles junto con sus cupos, packs y precio base."
    )
    public ResponseEntity<List<ClaseCabinaDisponibilidadResponse>> obtenerDisponibilidadCabinas(
            @PathVariable @Positive(message = "El ID del vuelo debe ser un número positivo") Long idVuelo,
            @Valid @ModelAttribute PasajerosBusquedaRequest request
    ) {
        var pasajeros = pasajerosBusquedaService.validarYResumir(request);
        List<ClaseCabinaDisponibilidadResponse> cabinas = busquedaVueloService.obtenerClasesCabinaDisponibles(
                idVuelo, pasajeros.asientosNecesarios());
        return ResponseEntity.ok(cabinas);
    }
}
