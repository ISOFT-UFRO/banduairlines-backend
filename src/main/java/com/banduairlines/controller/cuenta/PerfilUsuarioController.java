package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.RespuestaUsuario;
import com.banduairlines.dto.cuenta.SolicitudActualizacionPerfil;
import com.banduairlines.service.impl.PerfilUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cuentas/actual/perfil")
@PreAuthorize("hasAnyRole('PASAJERO', 'ADMINISTRADOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cuentas")
public class PerfilUsuarioController {
    private final PerfilUsuarioService perfiles;

    public PerfilUsuarioController(PerfilUsuarioService perfiles) {
        this.perfiles = perfiles;
    }

    @PatchMapping
    @Operation(summary = "Actualizar nombres y apellidos de la cuenta actual",
            description = "Acepta nombres, apellidos o ambos. Solo letras Unicode y espacios, hasta 80 caracteres por campo. La identidad se obtiene de la sesion; no requiere contrasena.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado"),
            @ApiResponse(responseCode = "400", description = "VALIDACION o PETICION_MALFORMADA"),
            @ApiResponse(responseCode = "401", description = "NO_AUTENTICADO"),
            @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO"),
            @ApiResponse(responseCode = "404", description = "CUENTA_NO_ENCONTRADA")
    })
    public RespuestaUsuario actualizar(@Valid @RequestBody SolicitudActualizacionPerfil solicitud) {
        return perfiles.actualizarActual(solicitud);
    }
}
