package com.banduairlines.service.tracking;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("tracking")
public record TrackingProperties(
        @DefaultValue("America/Santiago") ZoneId zonaHoraria,
        @DefaultValue("busqueda") String fuenteVuelos,
        @DefaultValue Proceso proceso,
        @DefaultValue Umbral umbral,
        @DefaultValue Sincronizacion sincronizacion
) {

    public record Proceso(
            @DefaultValue("true") boolean habilitado,
            @DefaultValue("60000") long intervaloMs
    ) {
    }

    public record Umbral(
            @DefaultValue("45") int embarqueAntesMin,
            @DefaultValue("0") int despegueDespuesMin,
            @DefaultValue("15") int enVueloDespuesMin
    ) {
    }

    public record Sincronizacion(
            @DefaultValue("1") int diasAtras,
            @DefaultValue("30") int diasAdelante
    ) {
    }
}
