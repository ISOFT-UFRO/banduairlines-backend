package com.banduairlines.controller.reportes;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.reportes.ResumenMensualResponse;
import com.banduairlines.service.reportes.ReporteService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/admin/reportes")
public class ReporteController {
    private final ReporteService reporteService;
    public ReporteController(ReporteService reporteService) { this.reporteService = reporteService; }

    @GetMapping("/historial")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Listar resúmenes mensuales históricos",
            description = "Entrega todos los periodos históricos disponibles para que el usuario seleccione uno y acceda posteriormente al detalle del RF4.")
    public ResponseEntity<List<ResumenMensualResponse>> listar() {
        return ResponseEntity.ok(reporteService.listar());
    }
}
