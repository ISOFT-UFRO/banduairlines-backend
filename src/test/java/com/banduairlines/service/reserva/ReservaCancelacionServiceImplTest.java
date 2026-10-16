package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoExtra;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.impl.ReservaCancelacionServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaCancelacionServiceImplTest {

    @Mock
    private ReservaAcceso reservaAcceso;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private PasajeroRepository pasajeroRepository;
    @Mock
    private BoletoRepository boletoRepository;

    private ReservaCancelacionServiceImpl service;
    private Reserva reserva;
    private Boleto boleto;

    @BeforeEach
    void setUp() {
        CalculadoraTotalReserva calculadora = new CalculadoraTotalReserva();
        service = new ReservaCancelacionServiceImpl(reservaAcceso, reservaRepository, pasajeroRepository, boletoRepository,
                new ResumenReserva(calculadora), calculadora);
        reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        Pasajero pasajero = CompraReservaFixtures.pendiente(1, reserva, TipoPasajero.ADULTO);
        TarifaVuelo tarifa = CompraReservaFixtures.tarifa(CompraReservaFixtures.vuelo(10, CompraReservaFixtures.SALIDA),
                ClaseCabina.ECONOMY, CompraReservaFixtures.packLight(), 50000);
        boleto = CompraReservaFixtures.boleto(1, reserva, pasajero, tarifa, 1);
        boleto.setAsientoId(CompraReservaFixtures.asiento(9, 2, "C", ClaseCabina.ECONOMY, true));
        boleto.addExtra(CompraReservaFixtures.extra(4, TipoExtra.ASIENTO_PREFERENTE, 6500), 1, BigDecimal.valueOf(6500));

        lenient().when(reservaAcceso.obtenerAutorizada(1L, "tok")).thenReturn(reserva);
        lenient().when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L)).thenReturn(List.of(boleto));
        lenient().when(pasajeroRepository.findByReserva_IdOrderByIdAsc(1L)).thenReturn(List.of(pasajero));
    }

    @Test
    void cancelar_pendientePago_quedaCanceladaYLiberaElAsiento() {
        ReservaResponse r = service.cancelar(1L, "tok");

        assertThat(r.estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(boleto.getAsientoId()).isNull();
        assertThat(boleto.getExtras()).isEmpty();
        assertThat(reserva.getTotal()).isEqualByComparingTo("50000");
        verify(reservaRepository).save(reserva);
    }

    @Test
    void cancelar_confirmadaOCancelada_lanzaEstadoInvalido() {
        for (EstadoReserva estado : List.of(EstadoReserva.CONFIRMADA, EstadoReserva.CANCELADA)) {
            reserva.setEstado(estado);
            assertThatThrownBy(() -> service.cancelar(1L, "tok"))
                    .isInstanceOfSatisfying(ConflictoException.class,
                            e -> assertThat(e.getCodigo()).isEqualTo(ReservaAcceso.ESTADO_INVALIDO));
        }
        verify(reservaRepository, never()).save(any());
    }
}
