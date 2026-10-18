package com.banduairlines.service.tracking.adaptador;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.TrackingBoletoLecturaRepository;
import com.banduairlines.repository.TrackingReservaLecturaRepository;
import com.banduairlines.service.tracking.puerto.VuelosDelUsuarioPort;

@Component
public class ReservaExternaReservaAdapter implements VuelosDelUsuarioPort {

    private final TrackingReservaLecturaRepository reservas;
    private final TrackingBoletoLecturaRepository boletos;

    public ReservaExternaReservaAdapter(TrackingReservaLecturaRepository reservas,
                                        TrackingBoletoLecturaRepository boletos) {
        this.reservas = reservas;
        this.boletos = boletos;
    }

    // Provisorio: reemplazar por el servicio de Cuentas (GET /cuentas/actual/vuelos) cuando esté en develop.
    @Override
    @Transactional(readOnly = true)
    public List<Long> vueloIdsDeUsuario(Long usuarioId) {
        return boletos.buscarVueloIdsPorReservas(reservas.buscarIdsPorUsuario(usuarioId, EstadoReserva.CONFIRMADA));
    }
}
