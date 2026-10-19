package com.banduairlines.dto.busqueda;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record ClaseCabinaDisponibilidadResponse(
        Long vueloId,
        String codigoVuelo,
        String claseCabina,
        int capacidadTotal,
        int asientosOcupados,
        int asientosDisponibles,
        boolean disponible,
        BigDecimal precioDesde,
        List<String> packsDisponibles
) {}