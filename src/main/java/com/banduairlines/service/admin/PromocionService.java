package com.banduairlines.service.admin;

import java.util.List;

import com.banduairlines.dto.admin.PromocionResponse;
import com.banduairlines.dto.admin.PromocionRequest;

public interface PromocionService {

    List<PromocionResponse> listar(Boolean activa);

    PromocionResponse obtener(Long id);

    PromocionResponse crear(PromocionRequest solicitud);

    PromocionResponse actualizar(Long id, PromocionRequest solicitud);

    PromocionResponse cambiarEstado(Long id, boolean activa);
}
