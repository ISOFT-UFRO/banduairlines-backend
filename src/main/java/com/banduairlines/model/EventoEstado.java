package com.banduairlines.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.OrigenEvento;

import java.time.LocalDateTime;

// Append-only (RNF-03): sin setters y solo SeguimientoVuelo crea eventos.
@Entity
@Immutable
@Table(name = "evento_estado", indexes = {
    @Index(name = "evento_estado_seguimiento_vuelo_id_fecha_cambio_index", columnList = "seguimiento_vuelo_id, fecha_cambio")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seguimiento_vuelo_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "evento_estado_seguimiento_vuelo_id_foreign"))
    private SeguimientoVuelo seguimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20, updatable = false)
    private EstadoVuelo estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20, updatable = false)
    private EstadoVuelo estadoNuevo;

    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    private LocalDateTime fechaCambio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private OrigenEvento origen;

    @Column(length = 500, updatable = false)
    private String motivo;

    // Solo auditoría: no se expone por API.
    @Column(name = "actualizado_por", updatable = false)
    private Long actualizadoPor;

    @Column(name = "retraso_minutos", updatable = false)
    private Integer retrasoMinutos;

    static EventoEstado registrar(SeguimientoVuelo seguimiento, EstadoVuelo estadoAnterior, EstadoVuelo estadoNuevo,
                                  LocalDateTime fechaCambio, OrigenEvento origen, String motivo,
                                  Long actualizadoPor, Integer retrasoMinutos) {
        EventoEstado evento = new EventoEstado();
        evento.seguimiento = seguimiento;
        evento.estadoAnterior = estadoAnterior;
        evento.estadoNuevo = estadoNuevo;
        evento.fechaCambio = fechaCambio;
        evento.origen = origen;
        evento.motivo = motivo;
        evento.actualizadoPor = actualizadoPor;
        evento.retrasoMinutos = retrasoMinutos;
        return evento;
    }
}
