package com.banduairlines.service.admin;

import com.banduairlines.dto.admin.EquipajePackResponse;
import com.banduairlines.dto.admin.EquipajePackRequest;

public interface EquipajePackService {

    EquipajePackResponse obtener(Long packId);

    EquipajePackResponse actualizar(
        Long packId,
        EquipajePackRequest solicitud
    );
}