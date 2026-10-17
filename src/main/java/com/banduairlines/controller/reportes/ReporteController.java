package com.banduairlines.controller.reportes;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.reportes.EstadisticasComercialesResponse;
import com.banduairlines.dto.reportes.FiltroEstadisticasRequest;
import com.banduairlines.service.reportes.ReporteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/reportes")
@Tag(
        name = "Reportes - Estadísticas Comerciales",
        description = "Indicadores comerciales consolidados de vuelos, reservas y pagos")
public class ReporteController {
    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService){
        this.reporteService = reporteService;
    }

    @GetMapping("/filtros")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            summary = "Consultar estadísticas comerciales",
            description = "Permite filtrar ventas, ingresos, ocupación, rutas, estados, pasajeros y tarifas.")
    public ResponseEntity<EstadisticasComercialesResponse> obtener(
            @Valid @ModelAttribute FiltroEstadisticasRequest filtro) {

        return ResponseEntity.ok(reporteService.obtener(filtro));
    }
}
