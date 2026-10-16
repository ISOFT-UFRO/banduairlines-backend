package com.banduairlines.service.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class RelojOperacionalTest {

    private static TrackingProperties propiedades(Map<String, String> valores) {
        return new Binder(new MapConfigurationPropertySource(valores))
                .bindOrCreate("tracking", TrackingProperties.class);
    }

    @Test
    void ahora_convierteElRelojUtcAHoraDeChile() {
        // 16-10-2026 rige el horario de verano de Chile (UTC-3).
        Clock utc = Clock.fixed(Instant.parse("2026-10-17T02:30:00Z"), ZoneOffset.UTC);

        RelojOperacional reloj = new RelojOperacional(utc, propiedades(Map.of()));

        assertThat(reloj.ahora()).isEqualTo(LocalDateTime.of(2026, 10, 16, 23, 30));
        assertThat(reloj.hoy()).isEqualTo(LocalDate.of(2026, 10, 16));
    }

    @Test
    void propiedades_sinConfiguracion_usanLosValoresPorDefecto() {
        TrackingProperties p = propiedades(Map.of());

        assertThat(p.zonaHoraria().getId()).isEqualTo("America/Santiago");
        assertThat(p.fuenteVuelos()).isEqualTo("busqueda");
        assertThat(p.proceso().habilitado()).isTrue();
        assertThat(p.proceso().intervaloMs()).isEqualTo(60000);
        assertThat(p.umbral().embarqueAntesMin()).isEqualTo(45);
        assertThat(p.umbral().despegueDespuesMin()).isZero();
        assertThat(p.umbral().enVueloDespuesMin()).isEqualTo(15);
        assertThat(p.sincronizacion().diasAtras()).isEqualTo(1);
        assertThat(p.sincronizacion().diasAdelante()).isEqualTo(30);
    }

    @Test
    void propiedades_aceptanValoresConfigurados() {
        TrackingProperties p = propiedades(Map.of(
                "tracking.zona-horaria", "UTC",
                "tracking.umbral.embarque-antes-min", "30",
                "tracking.proceso.habilitado", "false"));

        assertThat(p.zonaHoraria().getId()).isEqualTo("UTC");
        assertThat(p.umbral().embarqueAntesMin()).isEqualTo(30);
        assertThat(p.proceso().habilitado()).isFalse();
    }
}
