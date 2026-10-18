package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.RespuestaBeneficio;
import com.banduairlines.exception.ApiError;
import com.banduairlines.service.cuenta.CatalogoBeneficioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fidelizacion")
@PreAuthorize("hasRole('PASAJERO')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fidelización")
public class BeneficioController {

    private final CatalogoBeneficioService catalogoBeneficioService;

    public BeneficioController(CatalogoBeneficioService catalogoBeneficioService) {
        this.catalogoBeneficioService = catalogoBeneficioService;
    }

    @GetMapping("/beneficios")
    @Operation(
            summary = "Consultar el catálogo de beneficios",
            description = "Solo PASAJERO. Devuelve todo el catálogo configurado (app.fidelizacion.beneficios). Si el pasajero no puede canjear uno, "
                    + "disponible es false y motivoBloqueo indica por qué: BENEFICIO_AGOTADO (deshabilitado o sin stock), NIVEL_INSUFICIENTE "
                    + "o PUNTOS_INSUFICIENTES."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catálogo de beneficios"),
            @ApiResponse(responseCode = "401", description = "NO_AUTENTICADO", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO: el rol no participa en fidelización", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "502", description = "MILLAS_NO_DISPONIBLES o MOVIMIENTOS_NO_DISPONIBLES", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<RespuestaBeneficio> listar() {
        return catalogoBeneficioService.listarActual();
    }
}
