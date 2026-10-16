package com.banduairlines.service.admin;

import com.banduairlines.model.Asiento;
import com.banduairlines.model.enums.ClaseCabina;

import java.util.List;

public interface AsientoService {

    List<Asiento> obtenerAsientosPorAeronave(Long aeronaveId);

    List<Asiento> obtenerAsientosPorClase(
            Long aeronaveId,
            ClaseCabina claseCabina);

    Asiento crearAsiento(
            Long aeronaveId,
            Integer fila,
            String letra,
            ClaseCabina claseCabina,
            Boolean esPreferente);
}