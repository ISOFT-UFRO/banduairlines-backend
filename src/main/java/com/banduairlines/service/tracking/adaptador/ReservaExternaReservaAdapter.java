package com.banduairlines.service.tracking.adaptador;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.repository.TrackingBoletoLecturaRepository;
import com.banduairlines.repository.TrackingReservaLecturaRepository;
import com.banduairlines.service.tracking.puerto.ReservaExterna;
import com.banduairlines.service.tracking.puerto.ReservaExternaPort;

@Component
public class ReservaExternaReservaAdapter implements ReservaExternaPort {

    private final TrackingReservaLecturaRepository reservas;
    private final TrackingBoletoLecturaRepository boletos;

    public ReservaExternaReservaAdapter(TrackingReservaLecturaRepository reservas,
                                        TrackingBoletoLecturaRepository boletos) {
        this.reservas = reservas;
        this.boletos = boletos;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservaExterna> buscarPorCodigo(String codigoReserva) {
        return reservas.buscarPorCodigo(codigoReserva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> vueloIdsDeUsuario(Long usuarioId) {
        return boletos.buscarVueloIdsPorReservas(reservas.buscarIdsPorUsuario(usuarioId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> vueloIdsDeReserva(Long reservaId) {
        return boletos.buscarVueloIdsPorReservas(List.of(reservaId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean documentoPerteneceAReserva(Long reservaId, String numeroDocumento) {
        return boletos.contarPorReservaYDocumento(reservaId, numeroDocumento) > 0;
    }
}
