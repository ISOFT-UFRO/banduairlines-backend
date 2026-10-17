package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.RespuestaCanje;
import com.banduairlines.dto.cuenta.SolicitudCanje;
import com.banduairlines.exception.ApiError;
import com.banduairlines.service.cuenta.CanjeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fidelizacion")
@PreAuthorize("hasRole('PASAJERO')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fidelización")
public class CanjeController {

    public static final String ENCABEZADO_CLAVE = "Clave-Idempotencia";

    private final CanjeService canjeService;

    public CanjeController(CanjeService canjeService) {
        this.canjeService = canjeService;
    }

    @PostMapping("/canjes")
    @Operation(
            summary = "Canjear puntos por un beneficio",
            description = "Solo PASAJERO. Valida que el beneficio exista, esté disponible, que el nivel alcance y que "
                    + "haya puntos suficientes, y registra un movimiento CANJE. No modifica millas ni baja el nivel. "
                    + "Reintentos: si se repite la misma Clave-Idempotencia con el mismo beneficio, responde 200 con "
                    + "exactamente la misma respuesta del canje original (mismo idCanje, saldoAnterior, "
                    + "puntosDescontados, saldoActual y movimiento) y no descuenta puntos otra vez. Si la clave ya se "
                    + "usó con otro beneficio, responde 409 CANJE_DUPLICADO."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = SolicitudCanje.class),
                    examples = @ExampleObject(value = "{\"idBeneficio\":1}")
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Canje registrado"),
            @ApiResponse(responseCode = "200", description = "Reintento con la misma clave y el mismo beneficio: misma respuesta del canje original, sin nuevo descuento"),
            @ApiResponse(responseCode = "400", description = "VALIDACION: falta idBeneficio o la Clave-Idempotencia, o tiene un formato inválido", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "NO_AUTENTICADO", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO: el rol no participa en fidelización", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "BENEFICIO_NO_ENCONTRADO o CUENTA_NO_ENCONTRADA", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "CANJE_DUPLICADO: la clave ya se usó con otro beneficio", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "BENEFICIO_AGOTADO, NIVEL_INSUFICIENTE o PUNTOS_INSUFICIENTES", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "502", description = "MILLAS_NO_DISPONIBLES", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<RespuestaCanje> canjear(
            @Parameter(description = "Identificador único de la solicitud, por ejemplo un UUID. Entre 8 y 64 caracteres: letras, números o guiones.",
                    required = true, example = "3f2b6c1e-8d4a-4f0e-9b7a-1c2d3e4f5a6b")
            @RequestHeader(name = ENCABEZADO_CLAVE, required = false) String claveIdempotencia,
            @Valid @RequestBody SolicitudCanje solicitud) {
        CanjeService.ResultadoCanje resultado = canjeService.canjearActual(solicitud.idBeneficio(), claveIdempotencia);
        HttpStatus estado = resultado.creado() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(resultado.canje());
    }
}
