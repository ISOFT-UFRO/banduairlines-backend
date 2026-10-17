package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.service.impl.ReservaPasajerosServiceImpl;

class ResumenReservaTest {

    private final ResumenReserva resumen = new ResumenReserva(new CalculadoraTotalReserva());

    @Test
    void armar_antesDeRegistrarPasajeros_ocultaLosProvisoriosPeroCuentaLosDeclarados() {
        Reserva reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        List<Pasajero> pasajeros = List.of(
                CompraReservaFixtures.pendiente(1, reserva, TipoPasajero.ADULTO),
                CompraReservaFixtures.pendiente(2, reserva, TipoPasajero.NINO),
                CompraReservaFixtures.pendiente(3, reserva, TipoPasajero.BEBE));
        TarifaVuelo ida = CompraReservaFixtures.tarifa(CompraReservaFixtures.vuelo(10, CompraReservaFixtures.SALIDA),
                ClaseCabina.ECONOMY, CompraReservaFixtures.packLight(), 50000);
        TarifaVuelo vuelta = CompraReservaFixtures.tarifa(CompraReservaFixtures.vuelo(11, CompraReservaFixtures.SALIDA.plusDays(5)),
                ClaseCabina.ECONOMY, CompraReservaFixtures.packLight(), 40000);
        List<Boleto> boletos = new java.util.ArrayList<>(CompraReservaFixtures.boletos(reserva, pasajeros, ida, 1));
        boletos.addAll(CompraReservaFixtures.boletos(reserva, pasajeros, vuelta, 2));

        ReservaResponse r = resumen.armar(reserva, pasajeros, boletos);

        assertThat(r.codigoReserva()).isEqualTo("ABC234");
        assertThat(r.pasajerosRegistrados()).isFalse();
        assertThat(r.pasajeros()).isEmpty();
        assertThat(r.asientos()).isEmpty();
        assertThat(r.cantidadPasajeros()).isEqualTo(new ReservaResponse.CantidadPasajeros(1, 1, 1));
        assertThat(r.itinerario()).extracting(ReservaResponse.Tramo::tramoId).containsExactly(10L, 11L);
        assertThat(r.itinerario().get(0).origen()).isEqualTo("SCL");
        assertThat(r.contacto()).isNull();
        // 3 pasajeros (el bebe paga tarifa completa) x (50.000 + 40.000)
        assertThat(r.total().totalCLP()).isEqualTo(270000);
        assertThat(r.pack()).isEqualTo("Light");
    }

    @Test
    void armar_conPasajerosRealesYContacto_losMuestra() {
        Reserva reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        reserva.setEmailContacto("tomas@correo.cl");
        reserva.setTelefonoContacto("+56911112222");
        Pasajero adulto = CompraReservaFixtures.real(1, reserva, TipoPasajero.ADULTO, "Tomás", LocalDate.of(1990, 1, 1));
        TarifaVuelo ida = CompraReservaFixtures.tarifa(CompraReservaFixtures.vuelo(10, CompraReservaFixtures.SALIDA),
                ClaseCabina.ECONOMY, CompraReservaFixtures.packLight(), 50000);

        ReservaResponse r = resumen.armar(reserva, List.of(adulto), CompraReservaFixtures.boletos(reserva, List.of(adulto), ida, 1));

        assertThat(r.pasajerosRegistrados()).isTrue();
        assertThat(r.pasajeros()).extracting(p -> p.nombres()).containsExactly("Tomás");
        assertThat(r.asientos()).hasSize(1);
        assertThat(r.contacto().correo()).isEqualTo("tomas@correo.cl");
    }

    @Test
    void esPendiente_reconoceSoloLosProvisorios() {
        Reserva reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);

        assertThat(PasajerosPendientes.esPendiente(CompraReservaFixtures.pendiente(1, reserva, TipoPasajero.ADULTO))).isTrue();
        assertThat(PasajerosPendientes.esPendiente(
                CompraReservaFixtures.real(2, reserva, TipoPasajero.ADULTO, "Ana", LocalDate.of(1990, 1, 1)))).isFalse();
    }

    @Test
    void pendiente_tieneFechaDeNacimientoCoherenteConSuTipo() {
        Reserva reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        LocalDate vuelo = CompraReservaFixtures.SALIDA.toLocalDate();

        for (TipoPasajero tipo : TipoPasajero.values()) {
            Pasajero p = CompraReservaFixtures.pendiente(1, reserva, tipo);
            assertThat(ReservaPasajerosServiceImpl.tipoSegunEdad(p.getFechaNacimiento(), vuelo))
                    .isEqualTo(tipo);
        }
    }
}
