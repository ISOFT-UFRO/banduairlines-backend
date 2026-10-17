package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.reserva.ResultadoPagoRequest;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.impl.ReservaResultadoPagoServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaResultadoPagoServiceImplTest {

    @Mock private ReservaRepository reservaRepository;

    private ReservaResultadoPagoServiceImpl service;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        service = new ReservaResultadoPagoServiceImpl(reservaRepository, new TransaccionesPagoProvisional());
        reserva = Reserva.builder().id(1L).codigoReserva("ABC123").estado(EstadoReserva.PENDIENTE_PAGO)
                .total(new BigDecimal("150000")).build();
    }

    @Test
    void pagado_conElTotal_confirmaLaReserva() {
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));

        assertThat(service.registrar(1L, pagado("TX-1", 150000)).estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        verify(reservaRepository).save(reserva);
    }

    @Test
    void pagado_repetidoDeLaMismaTransaccion_respondeIgualSinVolverAConfirmar() {
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));
        service.registrar(1L, pagado("TX-1", 150000));

        assertThat(service.registrar(1L, pagado("TX-1", 150000)).estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        verify(reservaRepository, times(1)).save(reserva);
    }

    @Test
    void pagado_deOtraTransaccionSobreReservaConfirmada_lanzaEstadoInvalido() {
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));
        service.registrar(1L, pagado("TX-1", 150000));

        assertThatThrownBy(() -> service.registrar(1L, pagado("TX-2", 150000)))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(ReservaAcceso.ESTADO_INVALIDO));
    }

    @Test
    void pagado_montoDistinto_lanzaMontoNoCoincideSinConfirmar() {
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> service.registrar(1L, pagado("TX-1", 100000)))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("MONTO_NO_COINCIDE"));
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.PENDIENTE_PAGO);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void pagado_reservaCancelada_lanzaEstadoInvalido() {
        reserva.setEstado(EstadoReserva.CANCELADA);
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> service.registrar(1L, pagado("TX-1", 150000)))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(ReservaAcceso.ESTADO_INVALIDO));
    }

    @Test
    void rechazado_mantieneLaReservaPendienteDePago() {
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));
        ResultadoPagoRequest rechazo = new ResultadoPagoRequest("PAGO-1", null, ResultadoPagoRequest.Estado.RECHAZADO,
                150000L, OffsetDateTime.now(), "Fondos insuficientes");

        assertThat(service.registrar(1L, rechazo).estado()).isEqualTo(EstadoReserva.PENDIENTE_PAGO);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void reservaInexistente_lanza404() {
        when(reservaRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(99L, pagado("TX-1", 150000)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private static ResultadoPagoRequest pagado(String transaccionId, long montoCLP) {
        return new ResultadoPagoRequest("PAGO-1", transaccionId, ResultadoPagoRequest.Estado.PAGADO, montoCLP,
                OffsetDateTime.now(), null);
    }
}
