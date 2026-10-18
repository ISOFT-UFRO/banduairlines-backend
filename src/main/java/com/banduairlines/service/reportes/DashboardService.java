package com.banduairlines.service.reportes;

import com.banduairlines.dto.reportes.DashboardResponse;
import java.time.LocalDate;

public interface DashboardService {
    DashboardResponse obtener(LocalDate desde, LocalDate hasta, String agrupacion);
}
