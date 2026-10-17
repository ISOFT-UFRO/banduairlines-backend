package com.banduairlines.controller.busqueda;

import com.banduairlines.dto.busqueda.ConfiguracionCabinasResponse;
import com.banduairlines.dto.busqueda.ConfigurarCabinasRequest;
import com.banduairlines.service.ConfiguracionCabinaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/vuelos")
@RequiredArgsConstructor
public class AdminVueloController {

    // --- Dependencias ---

    private final ConfiguracionCabinaService configuracionCabinaService;

    // --- Endpoints ---

    @PutMapping("/{vueloId}/cabinas")
    public ResponseEntity<ConfiguracionCabinasResponse> configurarCabinas(
            @PathVariable Long vueloId,
            @Valid @RequestBody ConfigurarCabinasRequest request) {

        ConfiguracionCabinasResponse response = configuracionCabinaService.configurarCabinas(vueloId, request);
        return ResponseEntity.ok(response);
    }
}