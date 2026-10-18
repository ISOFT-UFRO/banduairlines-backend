package com.banduairlines.service.tracking;

import static com.banduairlines.model.enums.EstadoVuelo.ATERRIZADO;
import static com.banduairlines.model.enums.EstadoVuelo.CANCELADO;
import static com.banduairlines.model.enums.EstadoVuelo.DESPEGADO;
import static com.banduairlines.model.enums.EstadoVuelo.EMBARCANDO;
import static com.banduairlines.model.enums.EstadoVuelo.EN_VUELO;
import static com.banduairlines.model.enums.EstadoVuelo.PROGRAMADO;
import static com.banduairlines.model.enums.EstadoVuelo.RETRASADO;
import static com.banduairlines.model.enums.OrigenEvento.MANUAL;
import static com.banduairlines.model.enums.OrigenEvento.SISTEMA;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.banduairlines.model.EventoEstado;
import com.banduairlines.model.SeguimientoVuelo;

class EvolucionHorariaTest {

    private static final LocalDateTime SALIDA = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final LocalDateTime LLEGADA = LocalDateTime.of(2026, 10, 20, 12, 0);
    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 10, 16, 9, 0);
    private static final Long ADMIN = 7L;

    private final EvolucionHoraria evolucion = new EvolucionHoraria(PropiedadesDePrueba.porDefecto());
    private SeguimientoVuelo seguimiento;

    @BeforeEach
    void setUp() {
        seguimiento = SeguimientoVuelo.iniciar(42L, "BA-101", "SCL", "PMC", SALIDA, LLEGADA, CREACION);
    }

    private void reprogramar(int minutos) {
        seguimiento.sincronizar("BA-101", "SCL", "PMC", SALIDA.plusMinutes(minutos), LLEGADA.plusMinutes(minutos));
    }

    @Test
    void programado_antesDelUmbralDeEmbarque_noCambia() {
        assertThat(evolucion.aplicar(seguimiento, SALIDA.minusMinutes(46))).isEmpty();
        assertThat(seguimiento.getEstadoActual()).isEqualTo(PROGRAMADO);
    }

    @Test
    void programado_alLlegarAlUmbralDeEmbarque_pasaAEmbarcando() {
        evolucion.aplicar(seguimiento, SALIDA.minusMinutes(45));

        assertThat(seguimiento.getEstadoActual()).isEqualTo(EMBARCANDO);
    }

    @Test
    void avanza_enCadaUmbralConOrigenSistema() {
        evolucion.aplicar(seguimiento, SALIDA.minusMinutes(45));
        evolucion.aplicar(seguimiento, SALIDA.minusMinutes(1));
        assertThat(seguimiento.getEstadoActual()).isEqualTo(EMBARCANDO);

        evolucion.aplicar(seguimiento, SALIDA);
        evolucion.aplicar(seguimiento, SALIDA.plusMinutes(14));
        assertThat(seguimiento.getEstadoActual()).isEqualTo(DESPEGADO);

        evolucion.aplicar(seguimiento, SALIDA.plusMinutes(15));
        evolucion.aplicar(seguimiento, LLEGADA.minusMinutes(1));
        assertThat(seguimiento.getEstadoActual()).isEqualTo(EN_VUELO);

        evolucion.aplicar(seguimiento, LLEGADA);
        assertThat(seguimiento.getEstadoActual()).isEqualTo(ATERRIZADO);
        assertThat(seguimiento.getEventos()).hasSize(5).allSatisfy(e -> {
            assertThat(e.getOrigen()).isEqualTo(SISTEMA);
            assertThat(e.getActualizadoPor()).isNull();
            assertThat(e.getMotivo()).isNull();
        });
    }

    @Test
    void reprogramacion_generaRetrasadoDelSistemaConSuRetraso() {
        reprogramar(40);

        List<EventoEstado> eventos = evolucion.aplicar(seguimiento, CREACION.plusHours(1));

        assertThat(eventos).singleElement().satisfies(e -> {
            assertThat(e.getEstadoNuevo()).isEqualTo(RETRASADO);
            assertThat(e.getOrigen()).isEqualTo(SISTEMA);
            assertThat(e.getRetrasoMinutos()).isEqualTo(40);
            assertThat(e.getMotivo()).isEqualTo(
                    "Reprogramación detectada: la salida pasó de 20-10-2026 10:00 a 20-10-2026 10:40.");
        });
    }

    @Test
    void dosPasadasSinCambioDeRetraso_generanUnSoloEvento() {
        reprogramar(40);

        evolucion.aplicar(seguimiento, CREACION.plusHours(1));
        evolucion.aplicar(seguimiento, CREACION.plusHours(2));

        assertThat(seguimiento.getEventos()).hasSize(2);
    }

    @Test
    void retrasoQueCrece_generaOtroRetrasado() {
        reprogramar(40);
        evolucion.aplicar(seguimiento, CREACION.plusHours(1));
        reprogramar(70);
        evolucion.aplicar(seguimiento, CREACION.plusHours(2));

        assertThat(seguimiento.getEventos()).hasSize(3);
        assertThat(seguimiento.retrasoVigente()).isEqualTo(70);
        assertThat(seguimiento.getSalidaReferencia()).isEqualTo(SALIDA);
    }

    @Test
    void reprogramacionMenorQueElRetrasoManual_noGeneraEvento() {
        seguimiento.transicionar(RETRASADO, MANUAL, "Avión no llegó", ADMIN, 60, CREACION);
        reprogramar(40);

        assertThat(evolucion.aplicar(seguimiento, CREACION.plusHours(1))).isEmpty();
        assertThat(seguimiento.retrasoVigente()).isEqualTo(60);
    }

    @Test
    void reprogramacion_noAplicaDespuesDeEmbarcar() {
        evolucion.aplicar(seguimiento, SALIDA.minusMinutes(45));
        reprogramar(40);

        assertThat(evolucion.aplicar(seguimiento, SALIDA.minusMinutes(30))).isEmpty();
        assertThat(seguimiento.getEstadoActual()).isEqualTo(EMBARCANDO);
    }

    @Test
    void terminal_noSeToca() {
        seguimiento.transicionar(CANCELADO, MANUAL, "Falla técnica", ADMIN, null, CREACION);
        reprogramar(40);

        assertThat(evolucion.aplicar(seguimiento, LLEGADA.plusDays(1))).isEmpty();
        assertThat(seguimiento.getEstadoActual()).isEqualTo(CANCELADO);
    }

    @Test
    void retrasoManualSinReprogramacion_esperaLaNuevaHoraDeSalida() {
        seguimiento.transicionar(RETRASADO, MANUAL, "Avión no llegó", ADMIN, 60, CREACION);

        evolucion.aplicar(seguimiento, SALIDA.minusMinutes(30));
        assertThat(seguimiento.getEstadoActual()).isEqualTo(RETRASADO);

        evolucion.aplicar(seguimiento, SALIDA.plusMinutes(15));
        assertThat(seguimiento.getEstadoActual()).isEqualTo(EMBARCANDO);

        evolucion.aplicar(seguimiento, SALIDA.plusMinutes(59));
        assertThat(seguimiento.getEstadoActual()).isEqualTo(EMBARCANDO);
        evolucion.aplicar(seguimiento, SALIDA.plusMinutes(60));
        assertThat(seguimiento.getEstadoActual()).isEqualTo(DESPEGADO);
    }

    @Test
    void retrasoSinHoraEstimada_noAvanzaSolo() {
        seguimiento.transicionar(RETRASADO, MANUAL, "Sin hora estimada", ADMIN, null, CREACION);

        assertThat(evolucion.aplicar(seguimiento, LLEGADA.plusHours(5))).isEmpty();
        assertThat(seguimiento.getEstadoActual()).isEqualTo(RETRASADO);
    }

    @Test
    void vueloQueQuedoAtras_seRecuperaEnUnaSolaPasada() {
        List<EventoEstado> eventos = evolucion.aplicar(seguimiento, LLEGADA.plusMinutes(1));

        assertThat(eventos).extracting(EventoEstado::getEstadoNuevo)
                .containsExactly(EMBARCANDO, DESPEGADO, EN_VUELO, ATERRIZADO);
    }

    @Test
    void umbrales_sonConfigurables() {
        EvolucionHoraria conEmbarqueA30 = new EvolucionHoraria(
                PropiedadesDePrueba.con(Map.of("tracking.umbral.embarque-antes-min", "30")));

        assertThat(conEmbarqueA30.aplicar(seguimiento, SALIDA.minusMinutes(45))).isEmpty();
        conEmbarqueA30.aplicar(seguimiento, SALIDA.minusMinutes(30));
        assertThat(seguimiento.getEstadoActual()).isEqualTo(EMBARCANDO);
    }
}
