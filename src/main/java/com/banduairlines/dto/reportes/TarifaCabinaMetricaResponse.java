package com.banduairlines.dto.reportes;

import java.math.BigDecimal;
import com.banduairlines.model.enums.ClaseCabina;

public record TarifaCabinaMetricaResponse(
        Long packTarifarioId,
        String packTarifarioNombre,
        ClaseCabina claseCabina,
        Long cantidadBoletos,
        BigDecimal ingresosGenerados) {
}
