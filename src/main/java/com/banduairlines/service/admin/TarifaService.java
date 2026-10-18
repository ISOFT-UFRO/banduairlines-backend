package com.banduairlines.service.admin;

import java.math.BigDecimal;
import java.util.List;

import com.banduairlines.dto.admin.TarifaRequest;
import com.banduairlines.dto.admin.TarifaResponse;
import com.banduairlines.model.enums.ClaseCabina;

public interface TarifaService {

    List<TarifaResponse> listar(
            Long vueloId, ClaseCabina claseCabina);

    TarifaResponse obtener(Long vueloId, Long id);

    TarifaResponse crear(
            Long vueloId, TarifaRequest solicitud);

    TarifaResponse actualizarPrecio(
            Long vueloId, Long id, BigDecimal precio);
}