package com.banduairlines.service.tracking.adaptador;

import static com.banduairlines.model.enums.EstadoVuelo.ATERRIZADO;
import static com.banduairlines.model.enums.EstadoVuelo.DESPEGADO;
import static com.banduairlines.model.enums.EstadoVuelo.EMBARCANDO;
import static com.banduairlines.model.enums.EstadoVuelo.EN_VUELO;
import static com.banduairlines.model.enums.EstadoVuelo.PROGRAMADO;
import static com.banduairlines.model.enums.EstadoVuelo.RETRASADO;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.service.tracking.EvolucionHoraria;
import com.banduairlines.service.tracking.PropiedadesDePrueba;
import com.banduairlines.service.tracking.RelojOperacional;
import com.banduairlines.service.tracking.TrackingProperties;
import com.banduairlines.service.tracking.puerto.VueloExterno;

class VueloExternoDemoStubTest {

    // 15:00:30 UTC son las 12:00:30 en Chile; la base se trunca al minuto.
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 10, 16, 12, 0);
    private static final LocalDateTime DESDE = BASE.minusDays(1);
    private static final LocalDateTime HASTA = BASE.plusDays(30);

    private final TrackingProperties propiedades = PropiedadesDePrueba.porDefecto();
    private VueloExternoDemoStub stub;

    @BeforeEach
    void setUp() {
        Clock utc = Clock.fixed(Instant.parse("2026-10-16T15:00:30Z"), ZoneOffset.UTC);
        stub = new VueloExternoDemoStub(new RelojOperacional(utc, propiedades));
    }

    @Test
    void entregaSeisVuelosConIdsYCodigosDistintos() {
        List<VueloExterno> vuelos = stub.listarVuelosVigentes(DESDE, HASTA);

        assertThat(vuelos).hasSize(6);
        assertThat(vuelos).extracting(VueloExterno::vueloId).doesNotHaveDuplicates();
        assertThat(vuelos).extracting(VueloExterno::codigoVuelo).doesNotHaveDuplicates()
                .allSatisfy(codigo -> assertThat(codigo.length()).isLessThanOrEqualTo(10));
    }

    @Test
    void horarios_sonEstablesEntrePasadasSalvoUnaReprogramacionUnica() {
        List<VueloExterno> primera = stub.listarVuelosVigentes(DESDE, HASTA);
        List<VueloExterno> segunda = stub.listarVuelosVigentes(DESDE, HASTA);
        List<VueloExterno> tercera = stub.listarVuelosVigentes(DESDE, HASTA);

        assertThat(segunda.subList(0, 5)).isEqualTo(primera.subList(0, 5));
        assertThat(segunda.get(5).salidaProgramada())
                .isEqualTo(primera.get(5).salidaProgramada().plusMinutes(VueloExternoDemoStub.MINUTOS_REPROGRAMACION));
        assertThat(tercera).isEqualTo(segunda);
    }

    @Test
    void respetaLaVentanaPedida() {
        assertThat(stub.listarVuelosVigentes(BASE.minusHours(1), BASE.plusHours(1)))
                .extracting(VueloExterno::salidaProgramada)
                .containsExactlyInAnyOrder(BASE.plusMinutes(30), BASE.minusMinutes(10));
    }

    @Test
    void conLaEvolucionHoraria_muestraUnVueloPorEstado() {
        EvolucionHoraria evolucion = new EvolucionHoraria(propiedades);
        Map<Long, SeguimientoVuelo> seguimientos = new LinkedHashMap<>();

        for (VueloExterno v : stub.listarVuelosVigentes(DESDE, HASTA)) {
            SeguimientoVuelo s = SeguimientoVuelo.iniciar(v.vueloId(), v.codigoVuelo(), v.origenIata(),
                    v.destinoIata(), v.salidaProgramada(), v.llegadaProgramada(), BASE);
            evolucion.aplicar(s, BASE);
            seguimientos.put(v.vueloId(), s);
        }
        for (VueloExterno v : stub.listarVuelosVigentes(DESDE, HASTA)) {
            SeguimientoVuelo s = seguimientos.get(v.vueloId());
            s.sincronizar(v.codigoVuelo(), v.origenIata(), v.destinoIata(), v.salidaProgramada(), v.llegadaProgramada());
            evolucion.aplicar(s, BASE.plusMinutes(1));
        }

        assertThat(seguimientos.values()).extracting(SeguimientoVuelo::getEstadoActual)
                .containsExactly(PROGRAMADO, EMBARCANDO, DESPEGADO, EN_VUELO, ATERRIZADO, RETRASADO);
    }
}
