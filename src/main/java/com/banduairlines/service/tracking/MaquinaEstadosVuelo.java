package com.banduairlines.service.tracking;

import static com.banduairlines.model.enums.EstadoVuelo.ATERRIZADO;
import static com.banduairlines.model.enums.EstadoVuelo.CANCELADO;
import static com.banduairlines.model.enums.EstadoVuelo.DESPEGADO;
import static com.banduairlines.model.enums.EstadoVuelo.EMBARCANDO;
import static com.banduairlines.model.enums.EstadoVuelo.EN_VUELO;
import static com.banduairlines.model.enums.EstadoVuelo.PROGRAMADO;
import static com.banduairlines.model.enums.EstadoVuelo.RETRASADO;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.OrigenEvento;

public final class MaquinaEstadosVuelo {

    private static final Map<EstadoVuelo, Set<EstadoVuelo>> DESTINOS = new EnumMap<>(EstadoVuelo.class);

    static {
        // Sin retorno a PROGRAMADO: una reprogramación produce RETRASADO.
        DESTINOS.put(PROGRAMADO, EnumSet.of(EMBARCANDO, RETRASADO, CANCELADO));
        // RETRASADO -> RETRASADO registra un nuevo aplazamiento en el historial. A DESPEGADO solo vía EMBARCANDO.
        DESTINOS.put(RETRASADO, EnumSet.of(EMBARCANDO, RETRASADO, CANCELADO));
        DESTINOS.put(EMBARCANDO, EnumSet.of(DESPEGADO, RETRASADO, CANCELADO));
        // Tras despegar no hay RETRASADO: el atraso se deriva de la llegada.
        DESTINOS.put(DESPEGADO, EnumSet.of(EN_VUELO));
        DESTINOS.put(EN_VUELO, EnumSet.of(ATERRIZADO));
        // Terminales: un vuelo cancelado que se repone es un vuelo nuevo.
        DESTINOS.put(ATERRIZADO, EnumSet.noneOf(EstadoVuelo.class));
        DESTINOS.put(CANCELADO, EnumSet.noneOf(EstadoVuelo.class));
    }

    private static final Set<EstadoVuelo> SOLO_SISTEMA = Collections.unmodifiableSet(EnumSet.of(EN_VUELO, ATERRIZADO));

    private static final Set<EstadoVuelo> REQUIEREN_MOTIVO = Collections.unmodifiableSet(EnumSet.of(RETRASADO, CANCELADO));

    private MaquinaEstadosVuelo() {
    }

    public static boolean permite(EstadoVuelo actual, EstadoVuelo nuevo, OrigenEvento origen) {
        if (!DESTINOS.get(actual).contains(nuevo)) {
            return false;
        }
        return origen == OrigenEvento.SISTEMA || !SOLO_SISTEMA.contains(nuevo);
    }

    public static boolean requiereMotivo(EstadoVuelo nuevo) {
        return REQUIEREN_MOTIVO.contains(nuevo);
    }

    public static boolean esTerminal(EstadoVuelo estado) {
        return DESTINOS.get(estado).isEmpty();
    }

    public static void validar(EstadoVuelo actual, EstadoVuelo nuevo, OrigenEvento origen, String motivo) {
        if (!permite(actual, nuevo, origen)) {
            String mensaje = DESTINOS.get(actual).contains(nuevo)
                    ? "La transición de " + actual + " a " + nuevo + " solo la produce el sistema según el horario."
                    : "No se permite la transición de " + actual + " a " + nuevo + ".";
            throw new ReglaNegocioException(mensaje).conCodigo(CodigosTracking.TRANSICION_ESTADO_INVALIDA);
        }
        if (requiereMotivo(nuevo) && (motivo == null || motivo.isBlank())) {
            throw new ReglaNegocioException("El cambio a " + nuevo + " requiere un motivo.")
                    .conCodigo(CodigosTracking.MOTIVO_REQUERIDO);
        }
    }
}
