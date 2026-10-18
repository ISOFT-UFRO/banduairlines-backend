package com.banduairlines.controller.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.admin.PrecioTarifaRequest;
import com.banduairlines.dto.admin.TarifaRequest;
import com.banduairlines.dto.admin.TarifaResponse;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.service.admin.TarifaService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/vuelos/{vueloId}/tarifas")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class TarifaController {

    private final TarifaService tarifaService;

    @GetMapping
    public List<TarifaResponse> listar(
            @PathVariable @Positive Long vueloId,
            @RequestParam(required = false) ClaseCabina claseCabina) {

        return tarifaService.listar(vueloId, claseCabina);
    }

    @GetMapping("/{id}")
    public TarifaResponse obtener(
            @PathVariable @Positive Long vueloId,
            @PathVariable @Positive Long id) {

        return tarifaService.obtener(vueloId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TarifaResponse crear(
            @PathVariable @Positive Long vueloId,
            @Valid @RequestBody TarifaRequest solicitud) {

        return tarifaService.crear(vueloId, solicitud);
    }

    @PutMapping("/{id}/precio")
    public TarifaResponse actualizarPrecio(
            @PathVariable @Positive Long vueloId,
            @PathVariable @Positive Long id,
            @Valid @RequestBody PrecioTarifaRequest solicitud) {

        return tarifaService.actualizarPrecio(
                vueloId, id, solicitud.precio());
    }
}