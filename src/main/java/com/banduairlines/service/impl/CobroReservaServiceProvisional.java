package com.banduairlines.service.impl;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.banduairlines.model.Reserva;
import com.banduairlines.service.reserva.CobroReservaService;

/**
 * PROVISIONAL hasta que PAGOS entregue su integración con Flow: no registra el Pago (eso es de PAGOS)
 * ni devuelve URL de pasarela, solo permite probar el flujo de RESERVA de punta a punta.
 * Recuerda en memoria qué reservas iniciaron un cobro; se pierde al reiniciar la aplicación.
 * PAGOS borra esta clase al publicar su propia implementación de CobroReservaService, que debe
 * responder tieneCobroIniciado consultando sus pagos.
 */
@Service
public class CobroReservaServiceProvisional implements CobroReservaService {

    private final Set<Long> reservasConCobro = ConcurrentHashMap.newKeySet();

    @Override
    public CobroIniciado iniciarCobro(Reserva reserva, long montoCLP, String correo) {
        reservasConCobro.add(reserva.getId());
        return new CobroIniciado("PROVISIONAL-" + reserva.getId(), null);
    }

    @Override
    public boolean tieneCobroIniciado(Long reservaId) {
        return reservasConCobro.contains(reservaId);
    }
}
