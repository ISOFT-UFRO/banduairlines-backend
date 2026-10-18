package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.RespuestaSesion;
import com.banduairlines.dto.cuenta.SolicitudRenovacion;
import com.banduairlines.exception.ApiError;
import com.banduairlines.exception.CredencialRenovacionException;
import com.banduairlines.service.cuenta.RenovacionSesionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Cuentas", description = "Gestion de sesiones")
public class SesionController {

    private final RenovacionSesionService renovacionSesionService;
    private final com.banduairlines.service.impl.JwtService jwt;
    private final com.banduairlines.security.UsuarioActual usuarioActual;

    public SesionController(
            RenovacionSesionService renovacionSesionService, com.banduairlines.service.impl.JwtService jwt,
            com.banduairlines.security.UsuarioActual usuarioActual) {
        this.renovacionSesionService = renovacionSesionService;
        this.jwt = jwt;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping("/renovacion")
    @Operation(
            summary = "Renovar credenciales",
            description = "Valida un refresh JWT y emite "
                    + "un nuevo par de credenciales"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Credenciales renovadas"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credencial invalida o vencida"
            )
    })
    public ResponseEntity<RespuestaSesion> renovar(
            @Valid @RequestBody SolicitudRenovacion solicitud) {

        RespuestaSesion respuesta =
                renovacionSesionService.renovarSesion(solicitud);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(respuesta);
    }

    @PostMapping("/cierre-sesion")
    @Operation(
            summary = "Cerrar sesion",
            description = "Invalida las credenciales de acceso y renovacion de la sesion indicada."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Sesion cerrada"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Autenticacion requerida"
            )
    })
    public ResponseEntity<Void> cerrarSesion(
            @Valid @RequestBody SolicitudRenovacion solicitud) {

        try {
            jwt.cerrarSesion(solicitud.credencialRenovacion(), usuarioActual.exigirId());
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            throw new CredencialRenovacionException(true);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            throw new CredencialRenovacionException(false);
        }
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @ExceptionHandler(CredencialRenovacionException.class)
    public ResponseEntity<ApiError> errorRenovacion(
            CredencialRenovacionException ex,
            HttpServletRequest request) {

        String codigo = ex.estaVencida()
                ? "CREDENCIAL_RENOVACION_VENCIDA"
                : "CREDENCIAL_RENOVACION_INVALIDA";

        String mensaje = ex.estaVencida()
                ? "La credencial de renovacion ha vencido"
                : "La credencial de renovacion no es valida";

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                codigo,
                mensaje,
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error);
    }
}
