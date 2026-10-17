package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.SolicitudInicioSesion;
import com.banduairlines.dto.cuenta.RespuestaSesion;
import com.banduairlines.exception.ApiError;
import com.banduairlines.service.cuenta.InicioSesionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.CacheControl;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(
        name = "Cuentas",
        description = "Autenticacion de usuarios"
)
public class InicioSesionController {

    private final InicioSesionService inicioSesionService;

    public InicioSesionController(
            InicioSesionService inicioSesionService) {
        this.inicioSesionService = inicioSesionService;
    }

    @PostMapping("/inicio-sesion")
    @Operation(
            summary = "Iniciar sesion",
            description = "Autentica al usuario mediante correo "
                    + "y contraseña y genera credenciales JWT"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Inicio de sesion exitoso",
                    content = @Content(
                            schema = @Schema(
                                    implementation = RespuestaSesion.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "VALIDACION",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiError.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "CREDENCIALES_INVALIDAS",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiError.class
                            )
                    )
            )
    })
    public ResponseEntity<RespuestaSesion> iniciarSesion(
            @Valid @RequestBody SolicitudInicioSesion solicitud) {

        RespuestaSesion respuesta =
                inicioSesionService.iniciarSesion(solicitud);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(respuesta);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> credencialesInvalidas(
            BadCredentialsException ex,
            HttpServletRequest request) {

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "CREDENCIALES_INVALIDAS",
                "Correo o contraseña incorrectos",
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error);
    }
}
