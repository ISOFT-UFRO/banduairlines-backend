package com.banduairlines.dto.reportes;

import java.time.LocalDateTime;

public interface OcupacionVueloProjection {
    String getCodigoVuelo();
    String getOrigen();
    String getDestino();
    LocalDateTime getSalidaProgramada();
    long getAsientosOcupados();
    long getCapacidad();
}
