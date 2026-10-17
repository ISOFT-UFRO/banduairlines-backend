package com.banduairlines.service.tracking;

import static com.banduairlines.model.enums.EstadoVuelo.ATERRIZADO;
import static com.banduairlines.model.enums.EstadoVuelo.DESPEGADO;
import static com.banduairlines.model.enums.EstadoVuelo.EMBARCANDO;
import static com.banduairlines.model.enums.EstadoVuelo.EN_VUELO;
import static com.banduairlines.model.enums.EstadoVuelo.PROGRAMADO;
import static com.banduairlines.model.enums.EstadoVuelo.RETRASADO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.banduairlines.model.EventoEstado;
import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.OrigenEvento;

@Component
public class EvolucionHoraria {

    // Suficiente para recuperar en una pasada un vuelo que quedó atrás (PROGRAMADO -> ATERRIZADO son 4 pasos).
    static final int MAX_PASOS = 6;

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final TrackingProperties.Umbral umbral;

    public EvolucionHoraria(TrackingProperties properties) {
        this.umbral = properties.umbral();
    }

    public List<EventoEstado> aplicar(SeguimientoVuelo seguimiento, LocalDateTime ahora) {
        List<EventoEstado> eventos = new ArrayList<>();
        detectarReprogramacion(seguimiento, ahora).ifPresent(eventos::add);
        eventos.addAll(avanzar(seguimiento, ahora));
        return eventos;
    }

    Optional<EventoEstado> detectarReprogramacion(SeguimientoVuelo seguimiento, LocalDateTime ahora) {
        EstadoVuelo estado = seguimiento.getEstadoActual();
        if (estado != PROGRAMADO && estado != RETRASADO) {
            return Optional.empty();
        }
        // Solo si el retraso creció: un retraso sin cambios no genera evento y uno manual nunca se reduce.
        int calculado = seguimiento.retrasoPorReprogramacion();
        if (calculado <= seguimiento.retrasoVigente()) {
            return Optional.empty();
        }
        String motivo = "Reprogramación detectada: la salida pasó de "
                + seguimiento.getSalidaReferencia().format(FORMATO) + " a "
                + seguimiento.getSalidaProgramada().format(FORMATO) + ".";
        return Optional.of(seguimiento.transicionar(RETRASADO, OrigenEvento.SISTEMA, motivo, null, calculado, ahora));
    }

    List<EventoEstado> avanzar(SeguimientoVuelo seguimiento, LocalDateTime ahora) {
        List<EventoEstado> eventos = new ArrayList<>();
        for (int paso = 0; paso < MAX_PASOS; paso++) {
            Optional<EstadoVuelo> siguiente = siguienteEstado(seguimiento, ahora);
            if (siguiente.isEmpty()) {
                break;
            }
            eventos.add(seguimiento.transicionar(siguiente.get(), OrigenEvento.SISTEMA, null, null, null, ahora));
        }
        return eventos;
    }

    private Optional<EstadoVuelo> siguienteEstado(SeguimientoVuelo seguimiento, LocalDateTime ahora) {
        LocalDateTime salida = seguimiento.salidaEfectiva();
        return switch (seguimiento.getEstadoActual()) {
            case PROGRAMADO -> alcanzado(ahora, salida.minusMinutes(umbral.embarqueAntesMin()), EMBARCANDO);
            // Sin hora estimada (retraso 0) se espera al administrador o a una reprogramación.
            case RETRASADO -> seguimiento.retrasoVigente() > 0
                    ? alcanzado(ahora, salida.minusMinutes(umbral.embarqueAntesMin()), EMBARCANDO)
                    : Optional.empty();
            case EMBARCANDO -> alcanzado(ahora, salida.plusMinutes(umbral.despegueDespuesMin()), DESPEGADO);
            case DESPEGADO -> alcanzado(ahora, salida.plusMinutes(umbral.enVueloDespuesMin()), EN_VUELO);
            case EN_VUELO -> alcanzado(ahora, seguimiento.llegadaEfectiva(), ATERRIZADO);
            case ATERRIZADO, CANCELADO -> Optional.empty();
        };
    }

    private static Optional<EstadoVuelo> alcanzado(LocalDateTime ahora, LocalDateTime umbral, EstadoVuelo destino) {
        return ahora.isBefore(umbral) ? Optional.empty() : Optional.of(destino);
    }
}
