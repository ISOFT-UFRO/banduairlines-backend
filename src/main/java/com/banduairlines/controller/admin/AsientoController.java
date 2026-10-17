package com.banduairlines.controller.admin;

import com.banduairlines.dto.admin.AsientoResponse;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.service.admin.AsientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/asientos")
@RequiredArgsConstructor
public class AsientoController {

    private final AsientoService asientoService;

    @GetMapping("/aeronave/{aeronaveId}")
    public ResponseEntity<List<AsientoResponse>> listarPorAeronave(
            @PathVariable Long aeronaveId,
            @RequestParam(required = false) ClaseCabina claseCabina) {

        if (claseCabina != null) {
            return ResponseEntity.ok(
                    asientoService.obtenerAsientosPorClase(
                            aeronaveId, claseCabina));
        }

        return ResponseEntity.ok(
                asientoService.obtenerAsientosPorAeronave(aeronaveId));
    }

    @PostMapping
    public ResponseEntity<AsientoResponse> crearAsiento(
            @RequestParam Long aeronaveId,
            @RequestParam Integer fila,
            @RequestParam String letra,
            @RequestParam ClaseCabina claseCabina,
            @RequestParam Boolean esPreferente) {

        AsientoResponse respuesta = asientoService.crearAsiento(
                aeronaveId,
                fila,
                letra,
                claseCabina,
                esPreferente);

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}