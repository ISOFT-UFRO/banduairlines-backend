package com.banduairlines.controller.reportes;

import com.banduairlines.dto.reportes.EstadisticasRequest;
import com.banduairlines.dto.reportes.EstadisticasResponse;
import com.banduairlines.service.reportes.EstadisticasService;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reportes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    public EstadisticasController(
            EstadisticasService estadisticasService
    ) {
        this.estadisticasService = estadisticasService;
    }

    @GetMapping("/estadisticas")
    public EstadisticasResponse obtener(
            @Valid @ModelAttribute EstadisticasRequest request
    ) {
        return estadisticasService.obtener(
                request.desde(),
                request.hasta()
        );
    }
}