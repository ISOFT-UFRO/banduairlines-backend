package com.banduairlines.service.reportes;

import java.util.List;
import com.banduairlines.dto.reportes.EstadisticasComercialesResponse;
import com.banduairlines.dto.reportes.FiltroEstadisticasRequest;
import com.banduairlines.dto.reportes.ResumenMensualResponse;

public interface ReporteService {
    EstadisticasComercialesResponse obtener(FiltroEstadisticasRequest filtro);
    List<ResumenMensualResponse> listar();
}
