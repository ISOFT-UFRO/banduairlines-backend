package com.banduairlines.service.tracking;

import java.time.LocalDate;
import java.util.List;

import com.banduairlines.dto.tracking.ResumenVueloDTO;
import com.banduairlines.model.enums.EstadoVuelo;

public interface TrackingTableroService {

    /**
     * Retorna el tablero de vuelos del día (operativo), ordenado por salida programada ascendente.
     * Si {@code estado} es {@code null}, devuelve todos los vuelos del día.
     */
    List<ResumenVueloDTO> tableroDelDia(LocalDate fecha, EstadoVuelo estado);
}