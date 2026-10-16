package com.banduairlines.controller.busqueda;

import com.banduairlines.dto.busqueda.ClaseCabinaDisponibilidadResponse;
import com.banduairlines.dto.busqueda.ItinerarioVueloResponse;
import com.banduairlines.service.busqueda.BusquedaVueloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/busqueda")
@Validated
@Tag(name = "Búsqueda y Disponibilidad", description = "Endpoints para buscar alternativas de vuelos y disponibilidad de clases de cabina")
public class BusquedaVueloController {

    private final BusquedaVueloService busquedaVueloService;

    public BusquedaVueloController(BusquedaVueloService busquedaVueloService) {
        this.busquedaVueloService = busquedaVueloService;
    }

    @GetMapping("/itinerarios")
    @Operation(
            summary = "Buscar alternativas de vuelo (RF-03)",
            description = "Permite visualizar alternativas de vuelo directas y con una conexión (escala), indicando horarios y tramos involucrados."
    )
    public ResponseEntity<List<ItinerarioVueloResponse>> buscarItinerarios(
            @RequestParam @NotBlank(message = "El origen es obligatorio")
            @Size(min = 3, max = 3, message = "El código IATA de origen debe tener exactamente 3 caracteres") String origen,

            @RequestParam @NotBlank(message = "El destino es obligatorio")
            @Size(min = 3, max = 3, message = "El código IATA de destino debe tener exactamente 3 caracteres") String destino,

            @RequestParam @NotNull(message = "La fecha de salida es obligatoria")
            @FutureOrPresent(message = "La fecha de salida debe ser hoy o una fecha futura")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        List<ItinerarioVueloResponse> itinerarios = busquedaVueloService.obtenerAlternativasItinerario(
                origen,
                destino,
                fecha
        );
        return ResponseEntity.ok(itinerarios);
    }

    @GetMapping("/vuelos/{idVuelo}/disponibilidad")
    @Operation(
            summary = "Obtener clases de cabina disponibles por vuelo (RF-04)",
            description = "Permite visualizar, para una alternativa de vuelo específica, las clases de cabina disponibles junto con sus cupos, packs y precio base."
    )
    public ResponseEntity<List<ClaseCabinaDisponibilidadResponse>> obtenerDisponibilidadCabinas(
            @PathVariable @Positive(message = "El ID del vuelo debe ser un número positivo") Long idVuelo
    ) {
        List<ClaseCabinaDisponibilidadResponse> cabinas = busquedaVueloService.obtenerClasesCabinaDisponibles(idVuelo);
        return ResponseEntity.ok(cabinas);
    }
}