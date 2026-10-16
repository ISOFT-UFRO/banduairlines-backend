package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.service.impl.ReservaConsultaServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaConsultaServiceImplTest {

    @Mock
    private ReservaAcceso reservaAcceso;
    @Mock
    private PasajeroRepository pasajeroRepository;
    @Mock
    private BoletoRepository boletoRepository;

    private ReservaConsultaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReservaConsultaServiceImpl(reservaAcceso, pasajeroRepository, boletoRepository,
                new ResumenReserva(new CalculadoraTotalReserva()));
    }

    @Test
    void obtener_devuelveLaReservaConSuCodigoYTotal() {
        Reserva reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        List<Pasajero> pasajeros = List.of(CompraReservaFixtures.pendiente(1, reserva, TipoPasajero.ADULTO),
                CompraReservaFixtures.pendiente(2, reserva, TipoPasajero.ADULTO));
        TarifaVuelo tarifa = CompraReservaFixtures.tarifa(CompraReservaFixtures.vuelo(10, CompraReservaFixtures.SALIDA),
                ClaseCabina.ECONOMY, CompraReservaFixtures.packLight(), 50000);
        when(reservaAcceso.obtenerAutorizada(1L, "tok")).thenReturn(reserva);
        when(pasajeroRepository.findByReserva_IdOrderByIdAsc(1L)).thenReturn(pasajeros);
        when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L)).thenReturn(CompraReservaFixtures.boletos(reserva, pasajeros, tarifa, 1));

        ReservaResponse r = service.obtener(1L, "tok");

        assertThat(r.codigoReserva()).isEqualTo("ABC234");
        assertThat(r.cantidadPasajeros().adultos()).isEqualTo(2);
        assertThat(r.total().totalCLP()).isEqualTo(100000);
    }

    @Test
    void obtener_sinPermiso_propagaElError() {
        when(reservaAcceso.obtenerAutorizada(1L, null)).thenThrow(new AccessDeniedException("no"));

        assertThatThrownBy(() -> service.obtener(1L, null)).isInstanceOf(AccessDeniedException.class);
    }
}
