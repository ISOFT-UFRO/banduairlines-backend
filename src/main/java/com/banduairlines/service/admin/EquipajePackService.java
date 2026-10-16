package com.banduairlines.service.admin;

import com.banduairlines.dto.admin.EquipajePackRespuesta;
import com.banduairlines.dto.admin.EquipajePackSolicitud;

public interface EquipajePackService {

    EquipajePackRespuesta obtener(Long packId);

    EquipajePackRespuesta actualizar(
        Long packId,
        EquipajePackSolicitud solicitud
    );
}