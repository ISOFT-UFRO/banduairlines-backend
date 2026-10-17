package com.banduairlines.service.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.service.impl.TrackingTableroServiceImpl;

@ExtendWith(MockitoExtension.class)
class TrackingTableroServiceTest {

    @Mock
    private SeguimientoVueloRepository seguimientoRepository;

    private RelojOperacional relojOperacional;
    private TrackingTableroServiceImpl tableroService;

    @BeforeEach
    void setUp() {
        ZoneId zona = ZoneId.of("America/Santiago");
        // 2026-10-16T12:34:00-03:00 en Chile (aprox; no importa para rangos de día)
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T15:34:00Z"), ZoneId.of("UTC"));
        TrackingProperties props = PropiedadesDePrueba.con(java.util.Map.of(
                "tracking.zonaHoraria", zona.getId()
        ));
        relojOperacional = new RelojOperacional(clock, props);
        tableroService = new TrackingTableroServiceImpl(seguimientoRepository, relojOperacional);
    }

    @Test
    void tableroDelDia_sinFiltro_llamaRepositorioConRangoDelDiaYMapea() {
        LocalDateTime salida1 = LocalDateTime.of(2026, 10, 16, 9, 0);
        LocalDateTime salida2 = LocalDateTime.of(2026, 10, 16, 14, 0);
        SeguimientoVuelo s1 = org.mockito.Mockito.mock(SeguimientoVuelo.class, org.mockito.Mockito.withSettings().lenient());
        org.mockito.Mockito.when(s1.getId()).thenReturn(1L);
        org.mockito.Mockito.when(s1.getVueloId()).thenReturn(100L);
        org.mockito.Mockito.when(s1.getCodigoVuelo()).thenReturn("BD001");
        org.mockito.Mockito.when(s1.getOrigenIata()).thenReturn("SCL");
        org.mockito.Mockito.when(s1.getDestinoIata()).thenReturn("PMC");
        org.mockito.Mockito.when(s1.getSalidaProgramada()).thenReturn(salida1);
        org.mockito.Mockito.when(s1.getLlegadaProgramada()).thenReturn(salida1.plusHours(1));
        org.mockito.Mockito.when(s1.getEstadoActual()).thenReturn(EstadoVuelo.PROGRAMADO);
        org.mockito.Mockito.when(s1.retrasoVigente()).thenReturn(0);
        org.mockito.Mockito.when(s1.getUltimaActualizacion()).thenReturn(salida1.minusMinutes(5));

        SeguimientoVuelo s2 = org.mockito.Mockito.mock(SeguimientoVuelo.class, org.mockito.Mockito.withSettings().lenient());
        org.mockito.Mockito.when(s2.getId()).thenReturn(2L);
        org.mockito.Mockito.when(s2.getVueloId()).thenReturn(101L);
        org.mockito.Mockito.when(s2.getCodigoVuelo()).thenReturn("BD002");
        org.mockito.Mockito.when(s2.getOrigenIata()).thenReturn("SCL");
        org.mockito.Mockito.when(s2.getDestinoIata()).thenReturn("CJC");
        org.mockito.Mockito.when(s2.getSalidaProgramada()).thenReturn(salida2);
        org.mockito.Mockito.when(s2.getLlegadaProgramada()).thenReturn(salida2.plusHours(1));
        org.mockito.Mockito.when(s2.getEstadoActual()).thenReturn(EstadoVuelo.RETRASADO);
        org.mockito.Mockito.when(s2.retrasoVigente()).thenReturn(30);
        org.mockito.Mockito.when(s2.getUltimaActualizacion()).thenReturn(salida2.minusMinutes(10));

        when(seguimientoRepository.tablero(
                LocalDateTime.of(2026, 10, 16, 0, 0),
                LocalDateTime.of(2026, 10, 17, 0, 0),
                null))
                .thenReturn(List.of(s1, s2));

        var resultado = tableroService.tableroDelDia(null);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).codigoVuelo()).isEqualTo("BD001");
        assertThat(resultado.get(0).estadoActual()).isEqualTo(EstadoVuelo.PROGRAMADO);
        assertThat(resultado.get(1).codigoVuelo()).isEqualTo("BD002");
        assertThat(resultado.get(1).estadoActual()).isEqualTo(EstadoVuelo.RETRASADO);
        assertThat(resultado.get(1).retrasoMinutos()).isEqualTo(30);
    }

    @Test
    void tableroDelDia_conFiltroPorEstado_llamaRepositorioConEstado() {
        LocalDateTime salida = LocalDateTime.of(2026, 10, 16, 10, 0);
        SeguimientoVuelo s = org.mockito.Mockito.mock(SeguimientoVuelo.class, org.mockito.Mockito.withSettings().lenient());
        org.mockito.Mockito.when(s.getId()).thenReturn(3L);
        org.mockito.Mockito.when(s.getVueloId()).thenReturn(102L);
        org.mockito.Mockito.when(s.getCodigoVuelo()).thenReturn("BD003");
        org.mockito.Mockito.when(s.getOrigenIata()).thenReturn("SCL");
        org.mockito.Mockito.when(s.getDestinoIata()).thenReturn("PUQ");
        org.mockito.Mockito.when(s.getSalidaProgramada()).thenReturn(salida);
        org.mockito.Mockito.when(s.getLlegadaProgramada()).thenReturn(salida.plusHours(1));
        org.mockito.Mockito.when(s.getEstadoActual()).thenReturn(EstadoVuelo.EN_VUELO);
        org.mockito.Mockito.when(s.retrasoVigente()).thenReturn(0);
        org.mockito.Mockito.when(s.getUltimaActualizacion()).thenReturn(salida.minusMinutes(2));

        when(seguimientoRepository.tablero(
                LocalDateTime.of(2026, 10, 16, 0, 0),
                LocalDateTime.of(2026, 10, 17, 0, 0),
                EstadoVuelo.EN_VUELO))
                .thenReturn(List.of(s));

        var resultado = tableroService.tableroDelDia(EstadoVuelo.EN_VUELO);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).codigoVuelo()).isEqualTo("BD003");
        assertThat(resultado.get(0).estadoActual()).isEqualTo(EstadoVuelo.EN_VUELO);
    }

    @Test
    void tableroDelDia_sinResultados_devuelveListaVacia() {
        when(seguimientoRepository.tablero(
                LocalDateTime.of(2026, 10, 16, 0, 0),
                LocalDateTime.of(2026, 10, 17, 0, 0),
                null))
                .thenReturn(List.of());

        var resultado = tableroService.tableroDelDia(null);

        assertThat(resultado).isEmpty();
    }
}