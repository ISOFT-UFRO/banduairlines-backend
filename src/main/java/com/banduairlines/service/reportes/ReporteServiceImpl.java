package com.banduairlines.service.reportes;

import com.banduairlines.dto.reportes.ReporteResponse;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PagoRepository;
import com.banduairlines.repository.ReservaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ReporteServiceImpl implements ReporteService {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final BoletoRepository boletoRepository;

    public ReporteServiceImpl(
            PagoRepository pagoRepository,
            ReservaRepository reservaRepository,
            BoletoRepository boletoRepository
    ) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.boletoRepository = boletoRepository;
    }

    @Override
    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public ReporteResponse obtenerPorFecha(LocalDate fecha) {

        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();

        long ventas = reservaRepository.contarReservasReporte(
                EstadoReserva.CONFIRMADA,
                desde,
                hasta
        );

        long pendientes = reservaRepository.contarReservasReporte(
                EstadoReserva.PENDIENTE_PAGO,
                desde,
                hasta
        );

        BigDecimal ingresos = pagoRepository.sumarIngresosReporte(
                EstadoPago.PAGADO,
                "CLP",
                desde,
                hasta
        );

        if (ingresos == null) {
            ingresos = BigDecimal.ZERO;
        }

        long asientos = boletoRepository
                .contarAsientosAsignadosReporte(
                        EstadoReserva.CONFIRMADA,
                        desde,
                        hasta
                );

        long adultos = contarPasajeros(
                TipoPasajero.ADULTO, desde, hasta
        );

        long ninios = contarPasajeros(
                TipoPasajero.NINO, desde, hasta
        );

        long bebes = contarPasajeros(
                TipoPasajero.BEBE, desde, hasta
        );

        return new ReporteResponse(
                fecha,
                "CLP",
                ventas,
                ingresos,
                pendientes,
                asientos,
                adultos,
                ninios,
                bebes
        );
    }

    private long contarPasajeros(
            TipoPasajero tipo,
            LocalDateTime desde,
            LocalDateTime hasta
    ) {
        return boletoRepository.contarPasajerosReporte(
                EstadoReserva.CONFIRMADA,
                tipo,
                desde,
                hasta
        );
    }
}