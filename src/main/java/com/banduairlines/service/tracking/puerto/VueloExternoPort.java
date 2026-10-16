package com.banduairlines.service.tracking.puerto;

import java.time.LocalDateTime;
import java.util.List;

// Solo lo usa el proceso programado: las consultas de Tracking nunca leen de Búsqueda (RNF-01).
public interface VueloExternoPort {

    List<VueloExterno> listarVuelosVigentes(LocalDateTime desde, LocalDateTime hasta);
}
