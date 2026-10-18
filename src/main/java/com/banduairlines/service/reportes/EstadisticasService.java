package com.banduairlines.service.reportes;

import com.banduairlines.dto.reportes.EstadisticasResponse;

import java.time.LocalDate;

public interface EstadisticasService {

    EstadisticasResponse obtener(
            LocalDate desde,
            LocalDate hasta
    );
}