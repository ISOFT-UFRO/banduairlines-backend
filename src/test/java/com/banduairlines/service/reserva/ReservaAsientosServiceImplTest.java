package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.reserva.MapaAsientosResponse;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Asiento;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoExtra;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.CatalogoReservaRepository;
import com.banduairlines.service.impl.ReservaAsientosServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaAsientosServiceImplTest {

    @Mock
    private ReservaAcceso reservaAcceso;
    @Mock
    private BoletoRepository boletoRepository;
    @Mock
    private CatalogoReservaRepository catalogoReservaRepository;

    private ReservaAsientosServiceImpl service;
    private Reserva reserva;
    private Boleto boleto;

    private final Asiento business1A = CompraReservaFixtures.asiento(1, 1, "A", ClaseCabina.BUSINESS, true);
    private final Asiento pref5C = CompraReservaFixtures.asiento(2, 5, "C", ClaseCabina.ECONOMY, true);
    private final Asiento eco10A = CompraReservaFixtures.asiento(3, 10, "A", ClaseCabina.ECONOMY, false);
    private final Asiento eco10B = CompraReservaFixtures.asiento(4, 10, "B", ClaseCabina.ECONOMY, false);
    private final Asiento eco10C = CompraReservaFixtures.asiento(5, 10, "C", ClaseCabina.ECONOMY, false);

    @BeforeEach
    void setUp() {
        service = new ReservaAsientosServiceImpl(reservaAcceso, boletoRepository, catalogoReservaRepository,
                new AsientosReserva(catalogoReservaRepository));
        reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        usarPack(CompraReservaFixtures.packLight());

        lenient().when(reservaAcceso.obtenerAutorizada(1L, "tok")).thenReturn(reserva);
        lenient().when(reservaAcceso.ahora()).thenReturn(CompraReservaFixtures.AHORA);
        lenient().when(catalogoReservaRepository.asientosDeAeronave(5L)).thenReturn(List.of(business1A, pref5C, eco10A, eco10B, eco10C));
        lenient().when(boletoRepository.asientosTomadosPorOtras(eq(10L), eq(1L), any(), any(), any())).thenReturn(List.of(4L));
        lenient().when(catalogoReservaRepository.extrasActivos(TipoExtra.ASIENTO_PREFERENTE))
                .thenReturn(List.of(CompraReservaFixtures.extra(7, TipoExtra.ASIENTO_PREFERENTE, 6500)));
    }

    private void usarPack(PackTarifario pack) {
        Pasajero pasajero = CompraReservaFixtures.pendiente(1, reserva, TipoPasajero.ADULTO);
        TarifaVuelo tarifa = CompraReservaFixtures.tarifa(CompraReservaFixtures.vuelo(10, CompraReservaFixtures.SALIDA),
                ClaseCabina.ECONOMY, pack, 50000);
        boleto = CompraReservaFixtures.boleto(1, reserva, pasajero, tarifa, 1);
        lenient().when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L)).thenReturn(List.of(boleto));
    }

    private static Map<String, MapaAsientosResponse.Asiento> porCodigo(MapaAsientosResponse mapa) {
        return mapa.filas().stream().flatMap(f -> f.asientos().stream())
                .collect(Collectors.toMap(MapaAsientosResponse.Asiento::codigo, Function.identity()));
    }

    @Test
    void obtenerMapa_marcaEstadosYPrecios() {
        boleto.setAsientoId(eco10A);

        MapaAsientosResponse mapa = service.obtenerMapa(1L, 10L, "tok");
        Map<String, MapaAsientosResponse.Asiento> a = porCodigo(mapa);

        assertThat(mapa.tramoId()).isEqualTo(10L);
        assertThat(mapa.cabina()).isEqualTo(ClaseCabina.ECONOMY);
        assertThat(mapa.columnas()).containsExactly("A", "B", "C");
        assertThat(a.get("1A").estado()).isEqualTo("NO_DISPONIBLE"); // otra cabina
        assertThat(a.get("5C").estado()).isEqualTo("DISPONIBLE");
        assertThat(a.get("5C").precioCLP()).isEqualTo(6500);
        assertThat(a.get("5C").categoria()).isEqualTo(AsientosReserva.CATEGORIA_PREFERENTE);
        assertThat(a.get("10A").estado()).isEqualTo("SELECCIONADO");
        assertThat(a.get("10B").estado()).isEqualTo("NO_DISPONIBLE"); // tomado por otra compra
        assertThat(a.get("10C").estado()).isEqualTo("DISPONIBLE");
        assertThat(a.get("10C").precioCLP()).isZero();
    }

    @Test
    void obtenerMapa_packConSeleccionIncluida_preferenteGratis() {
        usarPack(CompraReservaFixtures.pack(2L, "Full", true));

        Map<String, MapaAsientosResponse.Asiento> a = porCodigo(service.obtenerMapa(1L, 10L, "tok"));

        assertThat(a.get("5C").precioCLP()).isZero();
        assertThat(a.get("5C").incluidoEnPack()).isTrue();
    }

    @Test
    void obtenerMapa_preferenteSinPrecioEnCatalogo_noDisponible() {
        when(catalogoReservaRepository.extrasActivos(TipoExtra.ASIENTO_PREFERENTE)).thenReturn(List.of());

        Map<String, MapaAsientosResponse.Asiento> a = porCodigo(service.obtenerMapa(1L, 10L, "tok"));

        assertThat(a.get("5C").estado()).isEqualTo("NO_DISPONIBLE");
        assertThat(a.get("10C").estado()).isEqualTo("DISPONIBLE");
    }

    @Test
    void obtenerMapa_tramoQueNoEsDeLaReserva_lanza404() {
        assertThatThrownBy(() -> service.obtenerMapa(1L, 99L, "tok")).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerMapa_sinPrecioPreferente_noFallaSiNoHayPreferentes() {
        when(catalogoReservaRepository.asientosDeAeronave(5L)).thenReturn(List.of(eco10A));
        when(catalogoReservaRepository.extrasActivos(TipoExtra.ASIENTO_PREFERENTE)).thenReturn(List.of());

        assertThat(porCodigo(service.obtenerMapa(1L, 10L, "tok"))).containsOnlyKeys("10A");
    }
}
