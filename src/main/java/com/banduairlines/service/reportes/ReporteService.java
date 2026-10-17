package com.banduairlines.service.reportes;

import java.util.List;
import java.time.LocalDate;

import com.banduairlines.dto.reportes.EstadisticasComercialesResponse;
import com.banduairlines.dto.reportes.FiltroEstadisticasRequest;
import com.banduairlines.dto.reportes.ResumenMensualResponse;
import com.banduairlines.dto.reportes.ReporteResponse;

public interface ReporteService {
    EstadisticasComercialesResponse obtener(FiltroEstadisticasRequest filtro);
    List<ResumenMensualResponse> listar();
    ReporteResponse obtenerPorFecha(LocalDate fecha);
}

