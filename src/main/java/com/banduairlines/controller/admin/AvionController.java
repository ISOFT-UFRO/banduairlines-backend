package com.banduairlines.controller.admin;

import com.banduairlines.dto.admin.AvionRequest;
import com.banduairlines.dto.admin.AvionResponse;
import com.banduairlines.service.admin.AvionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/aeronaves")
@RequiredArgsConstructor
public class AvionController {

    private final AvionService avionService;

    @PostMapping
    public ResponseEntity<AvionResponse> crearAvion(
            @Valid @RequestBody AvionRequest request) {

        AvionResponse respuesta = avionService.crearAvion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AvionResponse> obtenerAvion(
            @PathVariable Long id) {

        return ResponseEntity.ok(avionService.obtenerPorId(id));
    }
}