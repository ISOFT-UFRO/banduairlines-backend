package com.banduairlines.controller.reportes;

import com.banduairlines.dto.reportes.DashboardRequest;
import com.banduairlines.dto.reportes.DashboardResponse;
import com.banduairlines.service.reportes.DashboardService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reportes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse obtener(
            @Valid @ModelAttribute DashboardRequest request,
            @RequestParam(name = "agrupacion", defaultValue = "dia") String agrupacion) {
        return dashboardService.obtener(request.desde(), request.hasta(), agrupacion);
    }
}
