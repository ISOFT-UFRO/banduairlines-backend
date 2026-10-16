package com.banduairlines.model;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.service.tracking.CodigosTracking;

class SeguimientoVueloTest {

    private static final LocalDateTime SALIDA = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final LocalDateTime LLEGADA = LocalDateTime.of(2026, 10, 20, 12, 0);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 16, 9, 0);
    private static final Long ADMIN = 7L;

    private SeguimientoVuelo seguimiento;

    @BeforeEach
    void setUp() {
        seguimiento = SeguimientoVuelo.iniciar(42L, "BA-101", "SCL", "PMC", SALIDA, LLEGADA, AHORA);
    }

    @Test
    void iniciar_creaProgramadoConEventoDeCreacion() {
        assertThat(seguimiento.getEstadoActual()).isEqualTo(PROGRAMADO);
        assertThat(seguimiento.getSalidaReferencia()).isEqualTo(SALIDA);
        assertThat(seguimiento.getUltimaActualizacion()).isEqualTo(AHORA);
        assertThat(seguimiento.getEventos()).singleElement().satisfies(e -> {
            assertThat(e.getEstadoAnterior()).isNull();
            assertThat(e.getEstadoNuevo()).isEqualTo(PROGRAMADO);
            assertThat(e.getOrigen()).isEqualTo(SISTEMA);
            assertThat(e.getActualizadoPor()).isNull();
            assertThat(e.getMotivo()).isNull();
            assertThat(e.getSeguimiento()).isSameAs(seguimiento);
        });
    }

    @Test
    void transicionar_recorridoCompleto_generaCincoEventos() {
        seguimiento.transicionar(EMBARCANDO, SISTEMA, null, null, null, AHORA.plusMinutes(1));
        seguimiento.transicionar(DESPEGADO, SISTEMA, null, null, null, AHORA.plusMinutes(2));
        seguimiento.transicionar(EN_VUELO, SISTEMA, null, null, null, AHORA.plusMinutes(3));
        seguimiento.transicionar(ATERRIZADO, SISTEMA, null, null, null, AHORA.plusMinutes(4));

        assertThat(seguimiento.getEventos()).hasSize(5);
        assertThat(seguimiento.historial().get(0).getEstadoNuevo()).isEqualTo(ATERRIZADO);
        assertThat(seguimiento.getEstadoActual()).isEqualTo(ATERRIZADO);
        assertThat(seguimiento.getUltimaActualizacion()).isEqualTo(AHORA.plusMinutes(4));
        assertThat(seguimiento.esTerminal()).isTrue();
    }

    @Test
    void transicionar_retrasadoARetrasado_generaEventoNuevo() {
        seguimiento.transicionar(RETRASADO, MANUAL, "Clima", ADMIN, 30, AHORA);
        seguimiento.transicionar(RETRASADO, MANUAL, "Clima persiste", ADMIN, 90, AHORA.plusMinutes(5));

        assertThat(seguimiento.getEventos()).hasSize(3);
        assertThat(seguimiento.historial().get(0).getEstadoAnterior()).isEqualTo(RETRASADO);
        assertThat(seguimiento.retrasoVigente()).isEqualTo(90);
    }

    @Test
    void transicionar_canceladoSinMotivoOEnBlanco_lanzaMotivoRequerido() {
        assertThatThrownBy(() -> seguimiento.transicionar(CANCELADO, MANUAL, null, ADMIN, null, AHORA))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigosTracking.MOTIVO_REQUERIDO);
        assertThatThrownBy(() -> seguimiento.transicionar(CANCELADO, MANUAL, "   ", ADMIN, null, AHORA))
                .extracting("codigo").isEqualTo(CodigosTracking.MOTIVO_REQUERIDO);
    }

    @Test
    void transicionar_canceladoConMotivo_guardaMotivoRecortadoYAutor() {
        EventoEstado evento = seguimiento.transicionar(CANCELADO, MANUAL, "  Falla técnica  ", ADMIN, null, AHORA);

        assertThat(evento.getMotivo()).isEqualTo("Falla técnica");
        assertThat(evento.getActualizadoPor()).isEqualTo(ADMIN);
        assertThat(evento.getOrigen()).isEqualTo(MANUAL);
        assertThat(evento.getEstadoAnterior()).isEqualTo(PROGRAMADO);
        assertThat(seguimiento.getEstadoActual()).isEqualTo(CANCELADO);
    }

    @Test
    void transicionar_cuandoEstadoNoRequiereMotivo_loDescarta() {
        EventoEstado evento = seguimiento.transicionar(EMBARCANDO, MANUAL, "no corresponde", ADMIN, null, AHORA);

        assertThat(evento.getMotivo()).isNull();
    }

    @Test
    void transicionar_cuandoEsInvalida_noCambiaEstadoNiAgregaEvento() {
        assertThatThrownBy(() -> seguimiento.transicionar(ATERRIZADO, SISTEMA, null, null, null, AHORA))
                .extracting("codigo").isEqualTo(CodigosTracking.TRANSICION_ESTADO_INVALIDA);

        assertThat(seguimiento.getEstadoActual()).isEqualTo(PROGRAMADO);
        assertThat(seguimiento.getEventos()).hasSize(1);
    }

    @Test
    void transicionar_cuandoAutorNoCorrespondeAlOrigen_lanzaIllegalArgument() {
        assertThatThrownBy(() -> seguimiento.transicionar(EMBARCANDO, MANUAL, null, null, null, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> seguimiento.transicionar(EMBARCANDO, SISTEMA, null, ADMIN, null, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(seguimiento.getEventos()).hasSize(1);
    }

    @Test
    void transicionar_retrasoSoloSeGuardaEnRetrasado() {
        EventoEstado retrasado = seguimiento.transicionar(RETRASADO, MANUAL, "Clima", ADMIN, 30, AHORA);
        EventoEstado embarcando = seguimiento.transicionar(EMBARCANDO, SISTEMA, null, null, 99, AHORA.plusMinutes(1));

        assertThat(retrasado.getRetrasoMinutos()).isEqualTo(30);
        assertThat(embarcando.getRetrasoMinutos()).isNull();
        assertThat(seguimiento.retrasoVigente()).isEqualTo(30);
    }

    @Test
    void transicionar_retrasadoSinMinutos_usaLaReprogramacion() {
        EventoEstado sinReprogramar = seguimiento.transicionar(RETRASADO, MANUAL, "Sin hora estimada", ADMIN, null, AHORA);
        seguimiento.sincronizar("BA-101", "SCL", "PMC", SALIDA.plusMinutes(40), LLEGADA.plusMinutes(40));
        EventoEstado reprogramado = seguimiento.transicionar(RETRASADO, SISTEMA, "Reprogramación", null, null, AHORA.plusMinutes(1));

        assertThat(sinReprogramar.getRetrasoMinutos()).isZero();
        assertThat(reprogramado.getRetrasoMinutos()).isEqualTo(40);
    }

    @Test
    void sincronizar_nuncaCambiaLaSalidaDeReferencia() {
        seguimiento.sincronizar("BA-101", "SCL", "PMC", SALIDA.plusHours(1), LLEGADA.plusHours(1));
        seguimiento.sincronizar("BA-101", "SCL", "PMC", SALIDA.plusHours(3), LLEGADA.plusHours(3));

        assertThat(seguimiento.getSalidaReferencia()).isEqualTo(SALIDA);
        assertThat(seguimiento.getSalidaProgramada()).isEqualTo(SALIDA.plusHours(3));
    }

    @Test
    void salidaYLlegadaEfectivas_cuandoRetrasoManualSinReprogramacion_seDesplazan() {
        seguimiento.transicionar(RETRASADO, MANUAL, "Avión no llegó", ADMIN, 60, AHORA);

        assertThat(seguimiento.salidaEfectiva()).isEqualTo(SALIDA.plusMinutes(60));
        assertThat(seguimiento.llegadaEfectiva()).isEqualTo(LLEGADA.plusMinutes(60));
    }

    @Test
    void salidaYLlegadaEfectivas_cuandoLaReprogramacionYaIncluyeElRetraso_noLoSumanDosVeces() {
        seguimiento.sincronizar("BA-101", "SCL", "PMC", SALIDA.plusMinutes(40), LLEGADA.plusMinutes(40));
        seguimiento.transicionar(RETRASADO, SISTEMA, "Reprogramación", null, 40, AHORA);

        assertThat(seguimiento.salidaEfectiva()).isEqualTo(SALIDA.plusMinutes(40));
        assertThat(seguimiento.llegadaEfectiva()).isEqualTo(LLEGADA.plusMinutes(40));
    }

    @Test
    void salidaEfectiva_sinRetraso_esLaProgramada() {
        assertThat(seguimiento.retrasoVigente()).isZero();
        assertThat(seguimiento.salidaEfectiva()).isEqualTo(SALIDA);
        assertThat(seguimiento.llegadaEfectiva()).isEqualTo(LLEGADA);
    }

    @Test
    void historial_vaDelMasRecienteAlMasAntiguo() {
        seguimiento.transicionar(EMBARCANDO, SISTEMA, null, null, null, AHORA.plusMinutes(1));
        seguimiento.transicionar(DESPEGADO, SISTEMA, null, null, null, AHORA.plusMinutes(2));

        assertThat(seguimiento.historial()).extracting(EventoEstado::getEstadoNuevo)
                .containsExactly(DESPEGADO, EMBARCANDO, PROGRAMADO);
    }

    @Test
    void historial_conEventosEnElMismoInstante_conservaElOrdenDeCreacion() {
        seguimiento.transicionar(EMBARCANDO, SISTEMA, null, null, null, AHORA);
        seguimiento.transicionar(DESPEGADO, SISTEMA, null, null, null, AHORA);

        assertThat(seguimiento.historial()).extracting(EventoEstado::getEstadoNuevo)
                .containsExactly(DESPEGADO, EMBARCANDO, PROGRAMADO);
    }

    @Test
    void getEventos_noPermiteModificarElHistorial() {
        assertThatThrownBy(() -> seguimiento.getEventos().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
