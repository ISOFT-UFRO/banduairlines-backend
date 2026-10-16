package com.banduairlines.service.tracking;

import static com.banduairlines.model.enums.EstadoVuelo.EMBARCANDO;
import static com.banduairlines.model.enums.EstadoVuelo.PROGRAMADO;
import static com.banduairlines.model.enums.EstadoVuelo.RETRASADO;
import static com.banduairlines.model.enums.OrigenEvento.MANUAL;
import static com.banduairlines.model.enums.OrigenEvento.SISTEMA;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.banduairlines.dto.tracking.EventoEstadoDTO;
import com.banduairlines.dto.tracking.ResumenVueloDTO;
import com.banduairlines.dto.tracking.SeguimientoVueloDTO;
import com.banduairlines.model.SeguimientoVuelo;

class TrackingMapperTest {

    private static final LocalDateTime SALIDA = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final LocalDateTime LLEGADA = LocalDateTime.of(2026, 10, 20, 12, 0);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 20, 9, 0);

    private SeguimientoVuelo seguimientoConHistorial() {
        SeguimientoVuelo s = SeguimientoVuelo.iniciar(42L, "BA-101", "SCL", "PMC", SALIDA, LLEGADA, AHORA.minusDays(4));
        s.transicionar(RETRASADO, MANUAL, "Clima", 7L, 30, AHORA);
        s.transicionar(EMBARCANDO, SISTEMA, null, null, null, AHORA.plusMinutes(10));
        return s;
    }

    @Test
    void aSeguimientoDTO_copiaDatosRetrasoVigenteEHistorialDescendente() {
        SeguimientoVueloDTO dto = TrackingMapper.aSeguimientoDTO(seguimientoConHistorial());

        assertThat(dto.vueloId()).isEqualTo(42L);
        assertThat(dto.codigoVuelo()).isEqualTo("BA-101");
        assertThat(dto.origenIata()).isEqualTo("SCL");
        assertThat(dto.destinoIata()).isEqualTo("PMC");
        assertThat(dto.salidaProgramada()).isEqualTo(SALIDA);
        assertThat(dto.llegadaProgramada()).isEqualTo(LLEGADA);
        assertThat(dto.estadoActual()).isEqualTo(EMBARCANDO);
        assertThat(dto.retrasoMinutos()).isEqualTo(30);
        assertThat(dto.historial()).extracting(EventoEstadoDTO::estadoNuevo)
                .containsExactly(EMBARCANDO, RETRASADO, PROGRAMADO);
        assertThat(dto.historial().get(1).motivo()).isEqualTo("Clima");
        assertThat(dto.historial().get(2).estadoAnterior()).isNull();
    }

    @Test
    void aResumenDTO_copiaDatosSinHistorial() {
        ResumenVueloDTO dto = TrackingMapper.aResumenDTO(seguimientoConHistorial());

        assertThat(dto.codigoVuelo()).isEqualTo("BA-101");
        assertThat(dto.estadoActual()).isEqualTo(EMBARCANDO);
        assertThat(dto.retrasoMinutos()).isEqualTo(30);
        assertThat(dto.ultimaActualizacion()).isEqualTo(AHORA.plusMinutes(10));
    }

    @Test
    void ningunDTO_exponeQuienHizoElCambio() {
        for (Class<?> dto : new Class<?>[] { SeguimientoVueloDTO.class, EventoEstadoDTO.class, ResumenVueloDTO.class }) {
            assertThat(Arrays.stream(dto.getRecordComponents()).map(RecordComponent::getName))
                    .as(dto.getSimpleName())
                    .doesNotContain("actualizadoPor");
        }
    }
}
