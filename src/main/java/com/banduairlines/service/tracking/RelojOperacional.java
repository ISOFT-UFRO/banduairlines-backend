package com.banduairlines.service.tracking;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

// El Clock del proyecto es UTC y los horarios de Vuelo están en hora de Chile.
@Component
public class RelojOperacional {

    private final Clock clock;

    public RelojOperacional(Clock clock, TrackingProperties properties) {
        this.clock = clock.withZone(properties.zonaHoraria());
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }

    public LocalDate hoy() {
        return LocalDate.now(clock);
    }
}
