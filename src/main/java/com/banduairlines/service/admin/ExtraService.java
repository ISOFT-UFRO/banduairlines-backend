package com.banduairlines.service.admin;

import java.util.List;

import com.banduairlines.dto.admin.ExtraResponse;
import com.banduairlines.dto.admin.ExtraRequest;

public interface ExtraService {

    List<ExtraResponse> listar(Boolean activo);

    ExtraResponse obtener(Long id);

    ExtraResponse crear(ExtraRequest solicitud);

    ExtraResponse actualizar(Long id, ExtraRequest solicitud);

    ExtraResponse cambiarEstado(Long id, boolean activo);
}
