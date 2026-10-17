package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.RespuestaPaginada;
import com.banduairlines.dto.cuenta.RespuestaUsuario;
import com.banduairlines.dto.cuenta.SolicitudCreacionAdministrador;
import com.banduairlines.service.cuenta.AdministracionCuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/cuentas/administradores")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administracion de cuentas")
public class AdministracionCuentaController {

    private final AdministracionCuentaService administracion;

    public AdministracionCuentaController(AdministracionCuentaService administracion) {
        this.administracion = administracion;
    }

    @GetMapping
    @Operation(summary = "Listar administradores")
    public RespuestaPaginada<RespuestaUsuario> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return administracion.listarAdministradores(pagina, tamano);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un administrador")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Administrador creado"),
            @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO"),
            @ApiResponse(responseCode = "409", description = "CORREO_YA_REGISTRADO")
    })
    public RespuestaUsuario crear(@Valid @RequestBody SolicitudCreacionAdministrador solicitud) {
        return administracion.crearAdministrador(solicitud);
    }

    @GetMapping("/{administradorId}")
    @Operation(summary = "Consultar un administrador")
    public RespuestaUsuario obtener(@PathVariable Long administradorId) {
        return administracion.obtenerAdministrador(administradorId);
    }

    @DeleteMapping("/{administradorId}")
    @Operation(summary = "Eliminar un administrador")
    public ResponseEntity<Void> eliminar(@PathVariable Long administradorId) {
        administracion.eliminarAdministrador(administradorId);
        return ResponseEntity.noContent().build();
    }
}
