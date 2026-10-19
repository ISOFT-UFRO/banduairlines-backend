package com.banduairlines.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.OrigenEvento;
import com.banduairlines.service.tracking.MaquinaEstadosVuelo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "seguimiento_vuelo", indexes = {
    @Index(name = "seguimiento_vuelo_vuelo_id_unique", columnList = "vuelo_id", unique = true),
    // No único: el código puede repetirse entre tramos de un vuelo con escalas o reutilizarse otro día.
    @Index(name = "seguimiento_vuelo_codigo_vuelo_index", columnList = "codigo_vuelo"),
    @Index(name = "seguimiento_vuelo_salida_programada_index", columnList = "salida_programada")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeguimientoVuelo {

    private static final Comparator<EventoEstado> CRONOLOGICO = Comparator
            .comparing(EventoEstado::getFechaCambio)
            .thenComparing(EventoEstado::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sin FK: el vuelo pertenece a Búsqueda.
    @Column(name = "vuelo_id", nullable = false, updatable = false)
    private Long vueloId;

    @Column(name = "codigo_vuelo", nullable = false, length = 10)
    private String codigoVuelo;

    @Column(name = "origen_iata", nullable = false, length = 3)
    private String origenIata;

    @Column(name = "destino_iata", nullable = false, length = 3)
    private String destinoIata;

    // Primera salida conocida: Búsqueda sobrescribe la programada al reprogramar y sin esta no se mide el retraso.
    @Column(name = "salida_referencia", nullable = false, updatable = false)
    private LocalDateTime salidaReferencia;

    @Column(name = "salida_programada", nullable = false)
    private LocalDateTime salidaProgramada;

    @Column(name = "llegada_programada", nullable = false)
    private LocalDateTime llegadaProgramada;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_actual", nullable = false, length = 20)
    private EstadoVuelo estadoActual;

    @Column(name = "ultima_actualizacion", nullable = false)
    private LocalDateTime ultimaActualizacion;

    // Sin orphanRemoval: el historial no se borra.
    @OneToMany(mappedBy = "seguimiento", cascade = CascadeType.PERSIST)
    @OrderBy("id ASC")
    private List<EventoEstado> eventos = new ArrayList<>();

    public static SeguimientoVuelo iniciar(Long vueloId, String codigoVuelo, String origenIata, String destinoIata,
                                           LocalDateTime salidaProgramada, LocalDateTime llegadaProgramada,
                                           LocalDateTime ahora) {
        SeguimientoVuelo seguimiento = new SeguimientoVuelo();
        seguimiento.vueloId = Objects.requireNonNull(vueloId, "vueloId");
        seguimiento.sincronizar(codigoVuelo, origenIata, destinoIata, salidaProgramada, llegadaProgramada);
        seguimiento.salidaReferencia = salidaProgramada;
        seguimiento.estadoActual = EstadoVuelo.PROGRAMADO;
        seguimiento.ultimaActualizacion = Objects.requireNonNull(ahora, "ahora");
        seguimiento.eventos.add(EventoEstado.registrar(seguimiento, null, EstadoVuelo.PROGRAMADO, ahora,
                OrigenEvento.SISTEMA, null, null, null));
        return seguimiento;
    }

    public void sincronizar(String codigoVuelo, String origenIata, String destinoIata,
                            LocalDateTime salidaProgramada, LocalDateTime llegadaProgramada) {
        this.codigoVuelo = Objects.requireNonNull(codigoVuelo, "codigoVuelo");
        this.origenIata = Objects.requireNonNull(origenIata, "origenIata");
        this.destinoIata = Objects.requireNonNull(destinoIata, "destinoIata");
        this.salidaProgramada = Objects.requireNonNull(salidaProgramada, "salidaProgramada");
        this.llegadaProgramada = Objects.requireNonNull(llegadaProgramada, "llegadaProgramada");
    }

    // Única vía para cambiar el estado. Quien llama debe estar en una transacción.
    public EventoEstado transicionar(EstadoVuelo nuevo, OrigenEvento origen, String motivo,
                                     Long actualizadoPor, Integer retrasoMinutos, LocalDateTime ahora) {
        Objects.requireNonNull(nuevo, "nuevo");
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(ahora, "ahora");

        String motivoNormalizado = normalizarMotivo(nuevo, motivo);
        MaquinaEstadosVuelo.validar(estadoActual, nuevo, origen, motivoNormalizado);
        if ((origen == OrigenEvento.MANUAL) != (actualizadoPor != null)) {
            throw new IllegalArgumentException("actualizadoPor es obligatorio si y solo si el origen es MANUAL");
        }
        if (retrasoMinutos != null && retrasoMinutos < 0) {
            throw new IllegalArgumentException("retrasoMinutos no puede ser negativo");
        }

        Integer retraso = null;
        if (nuevo == EstadoVuelo.RETRASADO) {
            retraso = retrasoMinutos != null ? retrasoMinutos : retrasoPorReprogramacion();
        }

        EventoEstado evento = EventoEstado.registrar(this, estadoActual, nuevo, ahora, origen,
                motivoNormalizado, actualizadoPor, retraso);
        eventos.add(evento);
        estadoActual = nuevo;
        ultimaActualizacion = ahora;
        return evento;
    }

    public boolean esTerminal() {
        return MaquinaEstadosVuelo.esTerminal(estadoActual);
    }

    public int retrasoPorReprogramacion() {
        return (int) Math.max(0, Duration.between(salidaReferencia, salidaProgramada).toMinutes());
    }

    // El del evento más reciente que registra retraso, aunque después haya pasado a EMBARCANDO.
    public int retrasoVigente() {
        Integer vigente = null;
        for (EventoEstado evento : historialCronologico()) {
            if (evento.getRetrasoMinutos() != null) {
                vigente = evento.getRetrasoMinutos();
            }
        }
        return vigente != null ? vigente : 0;
    }

    // Considera un retraso que Búsqueda no reflejó en su programación (RETRASADO manual).
    public LocalDateTime salidaEfectiva() {
        LocalDateTime porRetraso = salidaReferencia.plusMinutes(retrasoVigente());
        return porRetraso.isAfter(salidaProgramada) ? porRetraso : salidaProgramada;
    }

    public LocalDateTime llegadaEfectiva() {
        return llegadaProgramada.plus(Duration.between(salidaProgramada, salidaEfectiva()));
    }

    public List<EventoEstado> historial() {
        List<EventoEstado> historial = historialCronologico();
        Collections.reverse(historial);
        return historial;
    }

    public List<EventoEstado> getEventos() {
        return Collections.unmodifiableList(eventos);
    }

    private List<EventoEstado> historialCronologico() {
        List<EventoEstado> ordenados = new ArrayList<>(eventos);
        ordenados.sort(CRONOLOGICO);
        return ordenados;
    }

    private static String normalizarMotivo(EstadoVuelo nuevo, String motivo) {
        if (!MaquinaEstadosVuelo.requiereMotivo(nuevo) || motivo == null || motivo.isBlank()) {
            return null;
        }
        return motivo.trim();
    }
}
