package com.banduairlines.controller.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.admin.EstadoRequest;
import com.banduairlines.dto.admin.PromocionResponse;
import com.banduairlines.dto.admin.PromocionRequest;
import com.banduairlines.service.admin.PromocionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/promociones")
public class PromocionController {

    private final PromocionService promocionService;

    public PromocionController(PromocionService promocionService) {
        this.promocionService = promocionService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<PromocionResponse> listar(@RequestParam(required = false) Boolean activa) {
        return promocionService.listar(activa);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PromocionResponse obtener(@PathVariable Long id) {
        return promocionService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PromocionResponse crear(@Valid @RequestBody PromocionRequest solicitud) {
        return promocionService.crear(solicitud);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PromocionResponse actualizar(@PathVariable Long id, @Valid @RequestBody PromocionRequest solicitud) {
        return promocionService.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PromocionResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest solicitud) {
        return promocionService.cambiarEstado(id, solicitud.activo());
    }
}
