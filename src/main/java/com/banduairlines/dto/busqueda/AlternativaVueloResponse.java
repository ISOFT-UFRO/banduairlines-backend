package com.banduairlines.dto.busqueda;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AlternativaVueloResponse(
        List<VueloBusquedaResponse> tramos,
        BigDecimal precioDesde,
        int cantidadEscalas,
        LocalDateTime salidaProgramada,
        LocalDateTime llegadaProgramada,
        long duracionTotalMin) {
}
