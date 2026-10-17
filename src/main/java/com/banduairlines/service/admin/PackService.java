package com.banduairlines.service.admin;

import java.util.List;

import com.banduairlines.dto.admin.PackRequest;
import com.banduairlines.dto.admin.PackResponse;

public interface PackService {

    List<PackResponse> listar(Boolean activo);

    PackResponse obtener(Long id);

    PackResponse crear(PackRequest solicitud);

    PackResponse actualizar(Long id, PackRequest solicitud);

    PackResponse cambiarEstado(Long id, boolean activo);
}