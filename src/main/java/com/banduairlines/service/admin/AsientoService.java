package com.banduairlines.service.admin;

import com.banduairlines.dto.admin.AsientoResponse;
import com.banduairlines.model.enums.ClaseCabina;

import java.util.List;

public interface AsientoService {

        List<AsientoResponse> obtenerAsientosPorAeronave(Long aeronaveId);

        List<AsientoResponse> obtenerAsientosPorClase(
                        Long aeronaveId,
                        ClaseCabina claseCabina);

        AsientoResponse crearAsiento(
                        Long aeronaveId,
                        Integer fila,
                        String letra,
                        ClaseCabina claseCabina,
                        Boolean esPreferente);
}