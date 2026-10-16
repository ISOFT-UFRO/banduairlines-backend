package com.banduairlines.controller.cuenta;

import com.banduairlines.service.cuenta.VuelosCuentaService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/cuentas/actual/vuelos")
@PreAuthorize("hasRole('PASAJERO')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cuentas")
public class VuelosCuentaController {
    private final VuelosCuentaService vuelos;

    public VuelosCuentaController(VuelosCuentaService vuelos) {
        this.vuelos = vuelos;
    }

    @GetMapping
    @Operation(summary = "Consultar identificadores de vuelos de la cuenta actual",
            description = "Devuelve IDs unicos de las reservas CONFIRMADA del pasajero autenticado, incluidos vuelos realizados. Sin vuelos devuelve una lista vacia. No consulta otros modulos por HTTP.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de identificadores numericos de vuelo"),
            @ApiResponse(responseCode = "401", description = "NO_AUTENTICADO"),
            @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO"),
            @ApiResponse(responseCode = "500", description = "ERROR_INTERNO")
    })
    public List<Long> consultar() {
        return vuelos.consultarActual();
    }
}
