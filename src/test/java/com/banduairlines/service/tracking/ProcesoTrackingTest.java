package com.banduairlines.service.tracking;

import static com.banduairlines.model.enums.EstadoVuelo.ATERRIZADO;
import static com.banduairlines.model.enums.EstadoVuelo.CANCELADO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.service.tracking.puerto.VueloExterno;
import com.banduairlines.service.tracking.puerto.VueloExternoPort;

@ExtendWith(MockitoExtension.class)
class ProcesoTrackingTest {

    // 15:00 UTC del 16-10-2026 son las 12:00 en Chile.
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 16, 12, 0);

    @Mock
    private VueloExternoPort vuelos;

    @Mock
    private SeguimientoVueloRepository seguimientos;

    @Mock
    private SincronizadorSeguimiento sincronizador;

    private ProcesoTracking proceso;

    @BeforeEach
    void setUp() {
        TrackingProperties propiedades = PropiedadesDePrueba.porDefecto();
        Clock utc = Clock.fixed(Instant.parse("2026-10-16T15:00:00Z"), ZoneOffset.UTC);
        proceso = new ProcesoTracking(vuelos, seguimientos, sincronizador,
                new RelojOperacional(utc, propiedades), propiedades);
    }

    private static VueloExterno vuelo(long id) {
        return new VueloExterno(id, "BA-" + id, "SCL", "PMC", AHORA.plusDays(1), AHORA.plusDays(1).plusHours(2));
    }

    private static SeguimientoVuelo seguimientoConId(long id) {
        SeguimientoVuelo s = SeguimientoVuelo.iniciar(id, "BA-" + id, "SCL", "PMC",
                AHORA.plusDays(1), AHORA.plusDays(1).plusHours(2), AHORA);
        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }

    @Test
    void pasada_pideLaVentanaDeVuelosVigentesYExcluyeTerminales() {
        when(vuelos.listarVuelosVigentes(any(), any())).thenReturn(List.of());
        when(seguimientos.findByEstadoActualNotIn(any())).thenReturn(List.of());

        proceso.ejecutarPasada();

        verify(vuelos).listarVuelosVigentes(AHORA.minusDays(1), AHORA.plusDays(30));
        verify(seguimientos).findByEstadoActualNotIn(EnumSet.of(ATERRIZADO, CANCELADO));
    }

    @Test
    void pasada_sincronizaYActualizaCadaVuelo() {
        when(vuelos.listarVuelosVigentes(any(), any())).thenReturn(List.of(vuelo(1), vuelo(2)));
        when(seguimientos.findByEstadoActualNotIn(any())).thenReturn(List.of(seguimientoConId(10)));

        proceso.ejecutarPasada();

        verify(sincronizador).sincronizar(vuelo(1), AHORA);
        verify(sincronizador).sincronizar(vuelo(2), AHORA);
        verify(sincronizador).actualizar(10L, AHORA);
    }

    @Test
    void pasada_cuandoUnVueloFalla_sigueConLosDemas() {
        when(vuelos.listarVuelosVigentes(any(), any())).thenReturn(List.of(vuelo(1), vuelo(2)));
        doThrow(new IllegalStateException("falla de prueba")).when(sincronizador).sincronizar(vuelo(1), AHORA);
        when(seguimientos.findByEstadoActualNotIn(any())).thenReturn(List.of(seguimientoConId(10), seguimientoConId(20)));
        doThrow(new IllegalStateException("falla de prueba")).when(sincronizador).actualizar(10L, AHORA);

        proceso.ejecutarPasada();

        verify(sincronizador).sincronizar(vuelo(2), AHORA);
        verify(sincronizador).actualizar(20L, AHORA);
    }
}
