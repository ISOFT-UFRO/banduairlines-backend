package com.banduairlines.service.reportes;

import com.banduairlines.dto.reportes.EstadisticasComercialesResponse;
import com.banduairlines.dto.reportes.FiltroEstadisticasRequest;

public interface ReporteService {
    EstadisticasComercialesResponse obtener(FiltroEstadisticasRequest filtro);
}
