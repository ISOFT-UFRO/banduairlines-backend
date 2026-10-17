package com.banduairlines.service.reserva;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * PROVISIONAL (endpoint 13): recuerda en memoria con qué transacción se confirmó cada reserva, para que un
 * PAGADO repetido de la misma transacción responda igual (idempotente) y uno de otra transacción responda 409.
 * Reemplaza a una columna reservas.transaccion_pago_id, que no se agrega sin la aprobación de Integración.
 *
 * Se pierde al reiniciar la aplicación: después de un reinicio, un PAGADO repetido para una reserva CONFIRMADA
 * responde 409 y PAGOS lo deriva a revisión manual; nunca se confirma una reserva dos veces.
 */
@Component
public class TransaccionesPagoProvisional {

    private final Map<Long, String> transaccionPorReserva = new ConcurrentHashMap<>();

    public void registrar(Long reservaId, String transaccionId) {
        transaccionPorReserva.put(reservaId, transaccionId);
    }

    /** true si la reserva se confirmó con esta misma transacción. */
    public boolean confirmadaCon(Long reservaId, String transaccionId) {
        return transaccionId != null && transaccionId.equals(transaccionPorReserva.get(reservaId));
    }
}
