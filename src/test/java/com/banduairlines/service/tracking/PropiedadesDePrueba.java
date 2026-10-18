package com.banduairlines.service.tracking;

import java.util.Map;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

public final class PropiedadesDePrueba {

    private PropiedadesDePrueba() {
    }

    public static TrackingProperties porDefecto() {
        return con(Map.of());
    }

    public static TrackingProperties con(Map<String, String> valores) {
        return new Binder(new MapConfigurationPropertySource(valores)).bindOrCreate("tracking", TrackingProperties.class);
    }
}
