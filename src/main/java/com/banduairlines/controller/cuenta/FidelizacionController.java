package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.RespuestaFidelizacion;
import com.banduairlines.dto.cuenta.RespuestaMovimientoPuntos;
import com.banduairlines.dto.cuenta.RespuestaPaginada;
import com.banduairlines.exception.ApiError;
import com.banduairlines.service.cuenta.FidelizacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cuentas/actual/fidelizacion")
@PreAuthorize("hasRole('PASAJERO')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fidelización", description = "Programa de pasajero frecuente: puntos, millas, nivel, beneficios y canjes")
public class FidelizacionController {

    private final FidelizacionService fidelizacionService;

    public FidelizacionController(FidelizacionService fidelizacionService) {
        this.fidelizacionService = fidelizacionService;
    }

    @GetMapping
    @Operation(
            summary = "Consultar el programa de pasajero frecuente",
            description = "Solo PASAJERO. Actualiza puntos, millas y nivel con los vuelos realizados y devuelve "
                    + "el saldo de puntos, las millas, el nivel, el multiplicador, el progreso al siguiente nivel, "
                    + "los beneficios que puede canjear ahora y los últimos 5 movimientos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen de fidelización"),
            @ApiResponse(responseCode = "401", description = "NO_AUTENTICADO", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO: el rol no participa en fidelización", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "CUENTA_NO_ENCONTRADA", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "502", description = "MILLAS_NO_DISPONIBLES", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public RespuestaFidelizacion consultar() {
        return fidelizacionService.consultarActual();
    }

    @GetMapping("/movimientos")
    @Operation(
            summary = "Listar el historial de puntos",
            description = "Solo PASAJERO. Acumulaciones y canjes paginados, del más reciente al más antiguo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de movimientos"),
            @ApiResponse(responseCode = "400", description = "VALIDACION: página o tamaño fuera de rango", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "NO_AUTENTICADO", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO: el rol no participa en fidelización", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "CUENTA_NO_ENCONTRADA", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public RespuestaPaginada<RespuestaMovimientoPuntos> listarMovimientos(
            @Parameter(description = "Número de página, desde 0")
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @Parameter(description = "Cantidad de movimientos por página, entre 1 y 100")
            @RequestParam(name = "tamano", defaultValue = "20") int tamano) {
        return fidelizacionService.listarMovimientosActual(pagina, tamano);
    }
}
