package com.banduairlines.controller.admin;

import com.banduairlines.dto.admin.EquipajePackRespuesta;
import com.banduairlines.dto.admin.EquipajePackSolicitud;
import com.banduairlines.service.admin.EquipajePackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/packs/{packId}/equipaje")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class EquipajePackController {

    private final EquipajePackService servicio;

    @GetMapping
    public EquipajePackRespuesta obtener(
            @PathVariable @Positive Long packId) {

        return servicio.obtener(packId);
    }

    @PutMapping
    public EquipajePackRespuesta actualizar(
            @PathVariable @Positive Long packId,
            @Valid @RequestBody EquipajePackSolicitud solicitud) {

        return servicio.actualizar(packId, solicitud);
    }
}