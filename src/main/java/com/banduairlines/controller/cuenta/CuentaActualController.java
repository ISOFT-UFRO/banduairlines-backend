package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.RespuestaUsuario;
import com.banduairlines.service.impl.PerfilUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cuentas/actual")
@PreAuthorize("hasAnyRole('PASAJERO', 'ADMINISTRADOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cuentas")
public class CuentaActualController {

    private final PerfilUsuarioService perfiles;

    public CuentaActualController(PerfilUsuarioService perfiles) {
        this.perfiles = perfiles;
    }

    @GetMapping
    @Operation(summary = "Consultar la cuenta autenticada")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos de la cuenta"),
            @ApiResponse(responseCode = "401", description = "NO_AUTENTICADO"),
            @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO"),
            @ApiResponse(responseCode = "404", description = "CUENTA_NO_ENCONTRADA")
    })
    public RespuestaUsuario consultar() {
        return perfiles.consultarActual();
    }
}
