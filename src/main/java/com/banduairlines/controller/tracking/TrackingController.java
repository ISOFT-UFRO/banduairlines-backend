package com.banduairlines.controller.tracking;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.tracking.SeguimientoVueloDTO;
import com.banduairlines.exception.ApiError;
import com.banduairlines.service.tracking.TrackingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/tracking/vuelos")
@Tag(name = "Tracking", description = "Consulta del estado operacional y del historial de vuelos")
@SecurityScheme(name = "trackingBearerAuth", type = SecuritySchemeType.HTTP,
        scheme = "bearer", bearerFormat = "JWT")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/{vueloId}")
    @PreAuthorize("hasAnyRole('PASAJERO', 'ADMINISTRADOR')")
    @Operation(summary = "Consultar el seguimiento de un vuelo por su identificador",
            description = "Devuelve la copia sincronizada del vuelo y su historial completo, "
                    + "ordenado desde el evento más reciente. No inicializa seguimientos.",
            security = @SecurityRequirement(name = "trackingBearerAuth"))
    @ApiResponse(responseCode = "200", description = "Seguimiento encontrado",
            content = @Content(schema = @Schema(implementation = SeguimientoVueloDTO.class)))
    @ApiResponse(responseCode = "400", description = "PETICION_MALFORMADA: el identificador no es un Long válido",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "NO_AUTENTICADO",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "VUELO_NO_ENCONTRADO: no hay seguimiento para el vuelo",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public SeguimientoVueloDTO obtenerPorId(@PathVariable Long vueloId) {
        return trackingService.obtenerPorId(vueloId);
    }
}
