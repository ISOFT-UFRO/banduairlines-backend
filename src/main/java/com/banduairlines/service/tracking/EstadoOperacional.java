package com.banduairlines.service.tracking;

import java.time.LocalDateTime;

import com.banduairlines.model.enums.EstadoVuelo;

// ultimaActualizacion: momento del último cambio de estado; en un vuelo ATERRIZADO es la hora de aterrizaje.
public record EstadoOperacional(EstadoVuelo estado, int retrasoMinutos, LocalDateTime ultimaActualizacion) {
}
