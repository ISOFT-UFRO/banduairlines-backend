package com.banduairlines.service.cuenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface FuenteVuelosFidelizacion {
    List<TramoRecorrido> consultar(Long idUsuario);

    record TramoRecorrido(Long idVuelo, String codigoPNR, LocalDateTime fechaLlegada,
                          BigDecimal distanciaKm) { }
}
