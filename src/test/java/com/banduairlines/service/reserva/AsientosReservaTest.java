package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.banduairlines.model.Asiento;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Extra;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoExtra;
import com.banduairlines.model.enums.TipoPasajero;

class AsientosReservaTest {

    private final Optional<Extra> preferente = Optional.of(CompraReservaFixtures.extra(4, TipoExtra.ASIENTO_PREFERENTE, 6500));

    @Test
    void precio_asientoNormalEsGratis() {
        Asiento normal = CompraReservaFixtures.asiento(1, 10, "A", ClaseCabina.ECONOMY, false);

        assertThat(AsientosReserva.precio(normal, CompraReservaFixtures.packLight(), preferente)).contains(0L);
        assertThat(AsientosReserva.categoria(normal)).isEqualTo(AsientosReserva.CATEGORIA_ESTANDAR);
    }

    @Test
    void precio_preferenteSeCobraSoloSiElPackNoIncluyeSeleccion() {
        Asiento pref = CompraReservaFixtures.asiento(1, 2, "C", ClaseCabina.ECONOMY, true);

        assertThat(AsientosReserva.precio(pref, CompraReservaFixtures.packLight(), preferente)).contains(6500L);
        assertThat(AsientosReserva.precio(pref, CompraReservaFixtures.pack(2L, "Full", true), preferente)).contains(0L);
        assertThat(AsientosReserva.incluidoEnPack(pref, CompraReservaFixtures.packLight())).isFalse();
    }

    @Test
    void precio_preferenteSinPrecioEnCatalogo_noSePuedeVender() {
        Asiento pref = CompraReservaFixtures.asiento(1, 2, "C", ClaseCabina.ECONOMY, true);

        assertThat(AsientosReserva.precio(pref, CompraReservaFixtures.packLight(), Optional.empty())).isEmpty();
    }

    @Test
    void columnasYPasillo_seisLetrasTienenPasilloDespuesDeC() {
        List<Asiento> asientos = List.of("F", "A", "C", "B", "E", "D").stream()
                .map(l -> CompraReservaFixtures.asiento(1, 1, l, ClaseCabina.ECONOMY, false)).toList();

        List<String> columnas = AsientosReserva.columnas(asientos);

        assertThat(columnas).containsExactly("A", "B", "C", "D", "E", "F");
        assertThat(AsientosReserva.pasillosDespuesDe(columnas)).containsExactly("C");
        assertThat(AsientosReserva.pasillosDespuesDe(List.of("A", "B", "C"))).isEmpty();
    }

    @Test
    void liberar_quitaElAsientoYSuCobro() {
        var reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        var tarifa = CompraReservaFixtures.tarifa(CompraReservaFixtures.vuelo(10, CompraReservaFixtures.SALIDA),
                ClaseCabina.ECONOMY, CompraReservaFixtures.packLight(), 50000);
        Boleto boleto = CompraReservaFixtures.boleto(1, reserva,
                CompraReservaFixtures.pendiente(1, reserva, TipoPasajero.ADULTO), tarifa, 1);
        boleto.setAsientoId(CompraReservaFixtures.asiento(9, 2, "C", ClaseCabina.ECONOMY, true));
        boleto.addExtra(preferente.get(), 1, BigDecimal.valueOf(6500));
        boleto.addExtra(CompraReservaFixtures.extra(1, TipoExtra.EQUIPAJE_BODEGA, 20000), 1, BigDecimal.valueOf(20000));

        AsientosReserva.liberar(boleto);

        assertThat(boleto.getAsientoId()).isNull();
        assertThat(boleto.getExtras()).extracting(be -> be.getExtra().getTipo()).containsExactly(TipoExtra.EQUIPAJE_BODEGA);
    }
}
