package com.banduairlines.controller.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
import com.banduairlines.dto.admin.PackRequest;
import com.banduairlines.dto.admin.PackResponse;
import com.banduairlines.service.admin.PackService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/packs")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class PackController {

    private final PackService packService;

    @GetMapping
    public List<PackResponse> listar(
            @RequestParam(required = false) Boolean activo) {

        return packService.listar(activo);
    }

    @GetMapping("/{id}")
    public PackResponse obtener(
            @PathVariable @Positive Long id) {

        return packService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PackResponse crear(
            @Valid @RequestBody PackRequest solicitud) {

        return packService.crear(solicitud);
    }

    @PutMapping("/{id}")
    public PackResponse actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody PackRequest solicitud) {

        return packService.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/activo")
    public PackResponse cambiarEstado(
            @PathVariable @Positive Long id,
            @Valid @RequestBody EstadoRequest solicitud) {

        return packService.cambiarEstado(id, solicitud.activo());
    }
}