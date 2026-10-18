package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.SolicitudEliminacionCuenta;
import com.banduairlines.service.impl.EliminacionCuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cuentas/actual")
@PreAuthorize("hasAnyRole('PASAJERO', 'ADMINISTRADOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cuentas")
public class EliminacionCuentaController {
    private final EliminacionCuentaService eliminacion;

    public EliminacionCuentaController(EliminacionCuentaService eliminacion) {
        this.eliminacion = eliminacion;
    }

    @DeleteMapping
    @Operation(summary = "Eliminar la cuenta propia",
            description = "Valida la contrasena, desactiva y anonimiza la cuenta. Conserva reservas, pagos, boletos y movimientos historicos. Revoca sesiones e invalida solicitudes al confirmar la transaccion. La notificacion es posterior; un fallo de correo no revierte la eliminacion.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cuenta anonimizada y desactivada"),
            @ApiResponse(responseCode = "400", description = "VALIDACION o PETICION_MALFORMADA"),
            @ApiResponse(responseCode = "401", description = "NO_AUTENTICADO"),
            @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO"),
            @ApiResponse(responseCode = "422", description = "CONTRASENA_ACTUAL_INCORRECTA")
    })
    public ResponseEntity<Void> eliminar(@Valid @RequestBody SolicitudEliminacionCuenta solicitud) {
        eliminacion.eliminarActual(solicitud);
        return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }
}
