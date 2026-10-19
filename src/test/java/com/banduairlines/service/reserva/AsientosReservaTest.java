package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
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
    void contiguos_vecinosEnLaMismaFilaSinPasillo() {
        List<Asiento> aeronave = new ArrayList<>();
        List.of("A", "C", "D", "F").forEach(l -> aeronave.add(asiento(1, l, ClaseCabina.BUSINESS)));
        for (int fila : List.of(5, 6)) {
            List.of("A", "B", "C", "D", "E", "F").forEach(l -> aeronave.add(asiento(fila, l, ClaseCabina.ECONOMY)));
        }

        assertThat(AsientosReserva.contiguos(List.of(eco(5, "A"), eco(5, "B"), eco(5, "C")), aeronave)).isTrue();
        assertThat(AsientosReserva.contiguos(List.of(eco(5, "C"), eco(5, "D")), aeronave)).isFalse(); // pasillo
        assertThat(AsientosReserva.contiguos(List.of(eco(5, "A"), eco(5, "C")), aeronave)).isFalse();
        assertThat(AsientosReserva.contiguos(List.of(eco(5, "A"), eco(6, "A")), aeronave)).isFalse();
        // En BUSINESS la fila solo tiene A, C, D y F: A y C quedan uno al lado del otro.
        assertThat(AsientosReserva.contiguos(List.of(asiento(1, "A", ClaseCabina.BUSINESS),
                asiento(1, "C", ClaseCabina.BUSINESS)), aeronave)).isTrue();
        assertThat(AsientosReserva.contiguos(List.of(asiento(1, "C", ClaseCabina.BUSINESS),
                asiento(1, "D", ClaseCabina.BUSINESS)), aeronave)).isFalse();
    }

    @Test
    void asignar_reemplazaSoloElCobroDelAsientoYConservaLosServicios() {
        Boleto boleto = boletoLight();
        boleto.addExtra(CompraReservaFixtures.extra(1, TipoExtra.EQUIPAJE_BODEGA, 20000), 1, BigDecimal.valueOf(20000));

        AsientosReserva.asignar(boleto, eco(10, "A"), preferente.get());
        assertThat(boleto.getExtras()).extracting(be -> be.getExtra().getTipo())
                .containsExactly(TipoExtra.EQUIPAJE_BODEGA, TipoExtra.ASIENTO_PREFERENTE);

        // Otro preferente con el precio vigente del catálogo: se actualiza el mismo registro, sin duplicarlo.
        AsientosReserva.asignar(boleto, eco(10, "B"), CompraReservaFixtures.extra(4, TipoExtra.ASIENTO_PREFERENTE, 7000));
        assertThat(boleto.getExtras()).hasSize(2);
        assertThat(boleto.getExtras().get(1).getPrecioUnitario()).isEqualByComparingTo("7000");

        AsientosReserva.asignar(boleto, eco(5, "A"), null);
        assertThat(boleto.getAsientoId().getFila()).isEqualTo(5);
        assertThat(boleto.getExtras()).extracting(be -> be.getExtra().getTipo()).containsExactly(TipoExtra.EQUIPAJE_BODEGA);

        AsientosReserva.asignar(boleto, null, preferente.get());
        assertThat(boleto.getAsientoId()).isNull();
        assertThat(boleto.getExtras()).extracting(be -> be.getExtra().getTipo()).containsExactly(TipoExtra.EQUIPAJE_BODEGA);
    }

    @Test
    void liberar_quitaElAsientoYSuCobro() {
        Boleto boleto = boletoLight();
        boleto.setAsientoId(CompraReservaFixtures.asiento(9, 2, "C", ClaseCabina.ECONOMY, true));
        boleto.addExtra(preferente.get(), 1, BigDecimal.valueOf(6500));
        boleto.addExtra(CompraReservaFixtures.extra(1, TipoExtra.EQUIPAJE_BODEGA, 20000), 1, BigDecimal.valueOf(20000));

        AsientosReserva.liberar(boleto);

        assertThat(boleto.getAsientoId()).isNull();
        assertThat(boleto.getExtras()).extracting(be -> be.getExtra().getTipo()).containsExactly(TipoExtra.EQUIPAJE_BODEGA);
    }

    private static Boleto boletoLight() {
        var reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        var tarifa = CompraReservaFixtures.tarifa(CompraReservaFixtures.vuelo(10, CompraReservaFixtures.SALIDA),
                ClaseCabina.ECONOMY, CompraReservaFixtures.packLight(), 50000);
        return CompraReservaFixtures.boleto(1, reserva, CompraReservaFixtures.pendiente(1, reserva, TipoPasajero.ADULTO), tarifa, 1);
    }

    private static Asiento eco(int fila, String letra) {
        return asiento(fila, letra, ClaseCabina.ECONOMY);
    }

    private static Asiento asiento(int fila, String letra, ClaseCabina cabina) {
        return CompraReservaFixtures.asiento(fila * 10L + (letra.charAt(0) - 'A'), fila, letra, cabina, false);
    }
}
