package com.banduairlines.controller.reportes;

import com.banduairlines.dto.reportes.ReporteResponse;
import com.banduairlines.service.reportes.ReporteService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/reportes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/detalle")
    public ReporteResponse obtenerDetalle(
            @RequestParam("fecha")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha
    ) {
        return reporteService.obtenerPorFecha(fecha);
    }
}