package com.banduairlines.service.reportes;

import java.util.List;

import com.banduairlines.dto.reportes.ResumenMensualResponse;

public interface ReporteService {
    List<ResumenMensualResponse> listar();
}
