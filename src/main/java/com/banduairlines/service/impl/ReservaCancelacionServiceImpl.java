package com.banduairlines.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.reserva.AsientosReserva;
import com.banduairlines.service.reserva.CalculadoraTotalReserva;
import com.banduairlines.service.reserva.ReservaAcceso;
import com.banduairlines.service.reserva.ReservaCancelacionService;
import com.banduairlines.service.reserva.ResumenReserva;

@Service
public class ReservaCancelacionServiceImpl implements ReservaCancelacionService {

    private final ReservaAcceso reservaAcceso;
    private final ReservaRepository reservaRepository;
    private final PasajeroRepository pasajeroRepository;
    private final BoletoRepository boletoRepository;
    private final ResumenReserva resumenReserva;
    private final CalculadoraTotalReserva calculadora;

    public ReservaCancelacionServiceImpl(ReservaAcceso reservaAcceso, ReservaRepository reservaRepository,
                                         PasajeroRepository pasajeroRepository, BoletoRepository boletoRepository,
                                         ResumenReserva resumenReserva,
                                         CalculadoraTotalReserva calculadora) {
        this.reservaAcceso = reservaAcceso;
        this.reservaRepository = reservaRepository;
        this.pasajeroRepository = pasajeroRepository;
        this.boletoRepository = boletoRepository;
        this.resumenReserva = resumenReserva;
        this.calculadora = calculadora;
    }

    /**
     * Solo una reserva PENDIENTE_PAGO se puede cancelar (aunque su bloqueo ya haya vencido).
     * Una CONFIRMADA ya esta pagada (su anulación es de PAGOS) y una CANCELADA no cambia: 409 ESTADO_INVALIDO.
     */
    @Override
    @Transactional
    public ReservaResponse cancelar(Long reservaId, String compraToken) {
        Reserva reserva = reservaAcceso.obtenerAutorizada(reservaId, compraToken);
        if (reserva.getEstado() != EstadoReserva.PENDIENTE_PAGO) {
            throw new ConflictoException("La reserva está " + reserva.getEstado() + " y no se puede cancelar")
                    .conCodigo(ReservaAcceso.ESTADO_INVALIDO);
        }
        List<Boleto> boletos = boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reserva.getId());
        cancelar(reserva, boletos);
        return resumenReserva.armar(reserva, pasajeroRepository.findByReserva_IdOrderByIdAsc(reserva.getId()), boletos);
    }

    /**
     * Los boletos se conservan como historial, pero sin asiento ni cobro de asiento: el asiento queda libre
     * y el cupo tambien, porque las consultas de disponibilidad no cuentan reservas CANCELADA
     */
    private void cancelar(Reserva reserva, List<Boleto> boletos) {
        reserva.setEstado(EstadoReserva.CANCELADA);
        boletos.forEach(AsientosReserva::liberar);
        calculadora.aplicar(reserva, calculadora.calcular(boletos, reserva.getPromocion(), false));
        reservaRepository.save(reserva);
    }
}
