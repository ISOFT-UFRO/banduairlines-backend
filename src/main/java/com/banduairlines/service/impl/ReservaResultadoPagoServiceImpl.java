package com.banduairlines.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.ResultadoPagoRequest;
import com.banduairlines.dto.reserva.ResultadoPagoResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.reserva.CalculadoraTotalReserva;
import com.banduairlines.service.reserva.ReservaAcceso;
import com.banduairlines.service.reserva.ReservaResultadoPagoService;
import com.banduairlines.service.reserva.TransaccionesPagoProvisional;

 //Endpoint 13, se dejo los import de los codigo de los endpoint hecho por otro para que el merge no halla problema
@Service
public class ReservaResultadoPagoServiceImpl implements ReservaResultadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(ReservaResultadoPagoServiceImpl.class);

    static final String MONTO_NO_COINCIDE = "MONTO_NO_COINCIDE";

    private final ReservaRepository reservaRepository;
    private final TransaccionesPagoProvisional transacciones;

    public ReservaResultadoPagoServiceImpl(ReservaRepository reservaRepository,
                                           TransaccionesPagoProvisional transacciones) {
        this.reservaRepository = reservaRepository;
        this.transacciones = transacciones;
    }

    @Override
    @Transactional
    public ResultadoPagoResponse registrar(Long reservaId, ResultadoPagoRequest request) {
        Reserva reserva = reservaRepository.findByIdForUpdate(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reservaId));
        return switch (request.estado()) {
            case PAGADO -> registrarPagado(reserva, request);
            case RECHAZADO -> registrarRechazado(reserva, request);
        };
    }

    private ResultadoPagoResponse registrarPagado(Reserva reserva, ResultadoPagoRequest request) {
        if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            if (transacciones.confirmadaCon(reserva.getId(), request.transaccionId())) {
                return respuesta(reserva); // reintento de PAGOS: idempotente
            }
            log.error("Pago {} (transacción {}) recibido para la reserva {}, que ya estaba CONFIRMADA con otra transacción; posible doble cobro",
                    request.pagoId(), request.transaccionId(), reserva.getId());
            throw new ConflictoException("La reserva ya fue confirmada con otra transacción")
                    .conCodigo(ReservaAcceso.ESTADO_INVALIDO);
        }
        if (reserva.getEstado() != EstadoReserva.PENDIENTE_PAGO) {
            log.error("Pago {} (transacción {}) recibido para la reserva {} en estado {}; revisar si corresponde reembolsar",
                    request.pagoId(), request.transaccionId(), reserva.getId(), reserva.getEstado());
            throw new ConflictoException("La reserva está " + reserva.getEstado() + " y no espera un pago")
                    .conCodigo(ReservaAcceso.ESTADO_INVALIDO);
        }
        long esperado = CalculadoraTotalReserva.clp(reserva.getTotal());
        if (esperado != request.montoCLP()) {
            log.error("Monto del pago {} ({} CLP) no coincide con el total de la reserva {} ({} CLP)",
                    request.pagoId(), request.montoCLP(), reserva.getId(), esperado);
            throw new ConflictoException("El monto pagado no coincide con el total de la reserva")
                    .conCodigo(MONTO_NO_COINCIDE);
        }
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.save(reserva);
        transacciones.registrar(reserva.getId(), request.transaccionId());
        return respuesta(reserva);
    }

    private ResultadoPagoResponse registrarRechazado(Reserva reserva, ResultadoPagoRequest request) {
        log.info("Pago {} rechazado para la reserva {} (estado {}): {}",
                request.pagoId(), reserva.getId(), reserva.getEstado(), request.motivoRechazo());
        return respuesta(reserva);
    }

    private static ResultadoPagoResponse respuesta(Reserva reserva) {
        return new ResultadoPagoResponse(reserva.getId(), reserva.getEstado());
    }
}