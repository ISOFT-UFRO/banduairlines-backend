package com.banduairlines.service.tracking;

import static com.banduairlines.model.enums.EstadoVuelo.EMBARCANDO;
import static com.banduairlines.model.enums.EstadoVuelo.PROGRAMADO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.service.tracking.puerto.VueloExterno;

@ExtendWith(MockitoExtension.class)
class SincronizadorSeguimientoTest {

    private static final LocalDateTime SALIDA = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final LocalDateTime LLEGADA = LocalDateTime.of(2026, 10, 20, 12, 0);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 16, 9, 0);

    @Mock
    private SeguimientoVueloRepository seguimientos;

    private SincronizadorSeguimiento sincronizador;

    @BeforeEach
    void setUp() {
        sincronizador = new SincronizadorSeguimiento(seguimientos, new EvolucionHoraria(PropiedadesDePrueba.porDefecto()));
    }

    @Test
    void sincronizar_vueloSinSeguimiento_creaUnoProgramado() {
        when(seguimientos.findByVueloId(42L)).thenReturn(Optional.empty());

        sincronizador.sincronizar(new VueloExterno(42L, "BA-101", "SCL", "PMC", SALIDA, LLEGADA), AHORA);

        ArgumentCaptor<SeguimientoVuelo> captor = ArgumentCaptor.forClass(SeguimientoVuelo.class);
        verify(seguimientos).save(captor.capture());
        SeguimientoVuelo creado = captor.getValue();
        assertThat(creado.getVueloId()).isEqualTo(42L);
        assertThat(creado.getEstadoActual()).isEqualTo(PROGRAMADO);
        assertThat(creado.getSalidaReferencia()).isEqualTo(SALIDA);
        assertThat(creado.getUltimaActualizacion()).isEqualTo(AHORA);
    }

    @Test
    void sincronizar_vueloConSeguimiento_refrescaLaCopiaSinTocarLaReferencia() {
        SeguimientoVuelo existente = SeguimientoVuelo.iniciar(42L, "BA-101", "SCL", "PMC", SALIDA, LLEGADA, AHORA);
        when(seguimientos.findByVueloId(42L)).thenReturn(Optional.of(existente));

        sincronizador.sincronizar(new VueloExterno(42L, "BA-102", "SCL", "PUQ",
                SALIDA.plusMinutes(40), LLEGADA.plusMinutes(40)), AHORA);

        verify(seguimientos, never()).save(any());
        assertThat(existente.getCodigoVuelo()).isEqualTo("BA-102");
        assertThat(existente.getDestinoIata()).isEqualTo("PUQ");
        assertThat(existente.getSalidaProgramada()).isEqualTo(SALIDA.plusMinutes(40));
        assertThat(existente.getSalidaReferencia()).isEqualTo(SALIDA);
    }

    @Test
    void actualizar_aplicaLaEvolucionPorHorario() {
        SeguimientoVuelo seguimiento = SeguimientoVuelo.iniciar(42L, "BA-101", "SCL", "PMC", SALIDA, LLEGADA, AHORA);
        when(seguimientos.buscarConHistorialPorId(1L)).thenReturn(Optional.of(seguimiento));

        sincronizador.actualizar(1L, SALIDA.minusMinutes(45));

        assertThat(seguimiento.getEstadoActual()).isEqualTo(EMBARCANDO);
    }

    @Test
    void actualizar_seguimientoInexistente_noHaceNada() {
        when(seguimientos.buscarConHistorialPorId(99L)).thenReturn(Optional.empty());

        assertThatCode(() -> sincronizador.actualizar(99L, AHORA)).doesNotThrowAnyException();
    }
}
