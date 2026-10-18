package com.banduairlines.service.tracking;

import static com.banduairlines.model.enums.EstadoVuelo.ATERRIZADO;
import static com.banduairlines.model.enums.EstadoVuelo.DESPEGADO;
import static com.banduairlines.model.enums.EstadoVuelo.EMBARCANDO;
import static com.banduairlines.model.enums.EstadoVuelo.EN_VUELO;
import static com.banduairlines.model.enums.EstadoVuelo.PROGRAMADO;
import static com.banduairlines.model.enums.EstadoVuelo.RETRASADO;
import static com.banduairlines.model.enums.OrigenEvento.MANUAL;
import static com.banduairlines.model.enums.OrigenEvento.SISTEMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.service.impl.TrackingEstadoOperacionalConsultaImpl;

@ExtendWith(MockitoExtension.class)
class TrackingEstadoOperacionalConsultaTest {

    private static final LocalDateTime SALIDA = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 20, 8, 0);

    @Mock
    private SeguimientoVueloRepository seguimientos;

    @InjectMocks
    private TrackingEstadoOperacionalConsultaImpl consulta;

    private static SeguimientoVuelo seguimiento(long vueloId) {
        return SeguimientoVuelo.iniciar(vueloId, "BA-" + vueloId, "SCL", "PMC", SALIDA, SALIDA.plusHours(2), AHORA);
    }

    @Test
    void estadosDeVuelos_entregaEstadoYRetrasoVigentePorVuelo() {
        SeguimientoVuelo programado = seguimiento(1L);
        SeguimientoVuelo retrasadoYEmbarcando = seguimiento(2L);
        retrasadoYEmbarcando.transicionar(RETRASADO, MANUAL, "Clima", 7L, 30, AHORA);
        retrasadoYEmbarcando.transicionar(EMBARCANDO, SISTEMA, null, null, null, AHORA.plusMinutes(5));
        when(seguimientos.findByVueloIdInOrderBySalidaProgramadaAsc(List.of(1L, 2L)))
                .thenReturn(List.of(programado, retrasadoYEmbarcando));

        var estados = consulta.estadosDeVuelos(List.of(1L, 2L));

        assertThat(estados).containsOnlyKeys(1L, 2L);
        assertThat(estados.get(1L)).isEqualTo(new EstadoOperacional(PROGRAMADO, 0, AHORA));
        assertThat(estados.get(2L)).isEqualTo(new EstadoOperacional(EMBARCANDO, 30, AHORA.plusMinutes(5)));
    }

    @Test
    void estadosDeVuelos_vueloAterrizado_entregaLaHoraDeAterrizaje() {
        SeguimientoVuelo aterrizado = seguimiento(3L);
        aterrizado.transicionar(EMBARCANDO, SISTEMA, null, null, null, SALIDA.minusMinutes(45));
        aterrizado.transicionar(DESPEGADO, SISTEMA, null, null, null, SALIDA);
        aterrizado.transicionar(EN_VUELO, SISTEMA, null, null, null, SALIDA.plusMinutes(15));
        aterrizado.transicionar(ATERRIZADO, SISTEMA, null, null, null, SALIDA.plusHours(2));
        when(seguimientos.findByVueloIdInOrderBySalidaProgramadaAsc(List.of(3L))).thenReturn(List.of(aterrizado));

        assertThat(consulta.estadosDeVuelos(List.of(3L)).get(3L))
                .isEqualTo(new EstadoOperacional(ATERRIZADO, 0, SALIDA.plusHours(2)));
    }

    @Test
    void estadosDeVuelos_vueloSinSeguimiento_noApareceEnElMapa() {
        when(seguimientos.findByVueloIdInOrderBySalidaProgramadaAsc(List.of(1L, 99L)))
                .thenReturn(List.of(seguimiento(1L)));

        assertThat(consulta.estadosDeVuelos(List.of(1L, 99L))).containsOnlyKeys(1L);
    }

    @Test
    void estadosDeVuelos_ignoraNulosYRepetidos() {
        when(seguimientos.findByVueloIdInOrderBySalidaProgramadaAsc(List.of(1L)))
                .thenReturn(List.of(seguimiento(1L)));

        assertThat(consulta.estadosDeVuelos(Arrays.asList(1L, null, 1L))).containsOnlyKeys(1L);
    }

    @Test
    void estadosDeVuelos_sinIds_noConsultaLaBase() {
        assertThat(consulta.estadosDeVuelos(List.of())).isEmpty();
        assertThat(consulta.estadosDeVuelos(null)).isEmpty();
        verify(seguimientos, never()).findByVueloIdInOrderBySalidaProgramadaAsc(any());
    }
}
