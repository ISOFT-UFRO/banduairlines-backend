package com.banduairlines.controller.tracking;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.tracking.ResumenVueloDTO;
import com.banduairlines.exception.ApiError;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.service.tracking.TrackingTableroService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin/tracking/tablero")
@Tag(name = "Tracking - Tablero", description = "Tablero de aeropuerto para personal autorizado")
@SecurityScheme(name = "trackingTableroBearerAuth", type = SecuritySchemeType.HTTP,
        scheme = "bearer", bearerFormat = "JWT")
public class TrackingTableroController {

    private final TrackingTableroService tableroService;

    public TrackingTableroController(TrackingTableroService tableroService) {
        this.tableroService = tableroService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Obtener el tablero de aeropuerto (vuelos del día)",
            description = "Consulta el estado actual de todos los vuelos del día. "
                    + "Ordenados por salida programada ascendente. "
                    + "Permite filtrar opcionalmente por estado del vuelo.",
            security = @SecurityRequirement(name = "trackingTableroBearerAuth"))
    @ApiResponse(responseCode = "200", description = "Listado del tablero",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResumenVueloDTO.class))))
    @ApiResponse(responseCode = "400", description = "PETICION_MALFORMADA: estado no válido",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "NO_AUTENTICADO",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public List<ResumenVueloDTO> tableroDelDia(
            @RequestParam(required = false) EstadoVuelo estado) {
        return tableroService.tableroDelDia(estado);
    }
}