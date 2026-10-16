package com.banduairlines.service.tracking;

import com.banduairlines.model.enums.EstadoVuelo;

public record EstadoOperacional(EstadoVuelo estado, int retrasoMinutos) {
}
