package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.reserva.AsientosAsignadosResponse;
import com.banduairlines.dto.reserva.AsignacionAsiento;
import com.banduairlines.dto.reserva.SeleccionarAsientosRequest;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Asiento;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Extra;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoExtra;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.CatalogoReservaRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.impl.ReservaAsientosServiceImpl;

/**
 * PUT /reservas/{reservaId}/asientos (6b): elegir y bloquear asientos (RF-01), con el menor junto a su adulto
 * responsable (RF-05). Reserva de un adulto, un niño a su cargo y un bebé en sus brazos, en un solo tramo.
 */
@ExtendWith(MockitoExtension.class)
class ReservaAsientosSeleccionTest {

    private static final long TARIFA = 29990;
    private static final long PREFERENTE = 5000;

    @Mock private ReservaAcceso reservaAcceso;
    @Mock private ReservaRepository reservaRepository;
    @Mock private BoletoRepository boletoRepository;
    @Mock private CatalogoReservaRepository catalogo;

    private ReservaAsientosServiceImpl service;
    private Reserva reserva;
    private PackTarifario pack;
    private TarifaVuelo tarifa;
    private Boleto boletoAdulto;
    private Boleto boletoNino;
    private List<Extra> preferentes;

    @BeforeEach
    void setUp() {
        service = new ReservaAsientosServiceImpl(reservaAcceso, reservaRepository, boletoRepository, catalogo,
                new AsientosReserva(catalogo), new CalculadoraTotalReserva());
        reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        pack = CompraReservaFixtures.packLight(); // sin selección de asiento incluida
        Vuelo vuelo = CompraReservaFixtures.vuelo(10, CompraReservaFixtures.SALIDA);
        tarifa = CompraReservaFixtures.tarifa(vuelo, ClaseCabina.ECONOMY, pack, TARIFA);

        Pasajero adulto = CompraReservaFixtures.real(1, reserva, TipoPasajero.ADULTO, "Ana", LocalDate.of(1990, 1, 1));
        Pasajero nino = CompraReservaFixtures.real(2, reserva, TipoPasajero.NINO, "Leo", LocalDate.of(2018, 3, 10));
        nino.setAdultoResponsable(adulto);
        Pasajero bebe = CompraReservaFixtures.real(3, reserva, TipoPasajero.BEBE, "Sol", LocalDate.of(2026, 2, 1));
        bebe.setAdultoResponsable(adulto);
        boletoAdulto = CompraReservaFixtures.boleto(100, reserva, adulto, tarifa, 1);
        boletoNino = CompraReservaFixtures.boleto(200, reserva, nino, tarifa, 1);
        Boleto boletoBebe = CompraReservaFixtures.boleto(300, reserva, bebe, tarifa, 1);

        // Fila 1 BUSINESS (A C | D F); filas 5 y 6 ECONOMY normales y fila 10 ECONOMY preferente (A B C | D E F).
        List<Asiento> asientos = new ArrayList<>();
        for (String letra : List.of("A", "C", "D", "F")) {
            asientos.add(CompraReservaFixtures.asiento(id(1, letra), 1, letra, ClaseCabina.BUSINESS, true));
        }
        for (int fila : List.of(5, 6, 10)) {
            for (String letra : List.of("A", "B", "C", "D", "E", "F")) {
                asientos.add(CompraReservaFixtures.asiento(id(fila, letra), fila, letra, ClaseCabina.ECONOMY, fila == 10));
            }
        }
        preferentes = new ArrayList<>(List.of(CompraReservaFixtures.extra(51, TipoExtra.ASIENTO_PREFERENTE, PREFERENTE)));

        lenient().when(reservaAcceso.obtenerAutorizada(1L, "tok")).thenReturn(reserva);
        lenient().when(reservaAcceso.ahora()).thenReturn(CompraReservaFixtures.AHORA);
        lenient().when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L))
                .thenReturn(List.of(boletoAdulto, boletoNino, boletoBebe));
        lenient().when(catalogo.asientosDeAeronave(5L)).thenReturn(asientos);
        lenient().when(catalogo.extrasActivos(TipoExtra.ASIENTO_PREFERENTE)).thenAnswer(inv -> List.copyOf(preferentes));
        lenient().when(catalogo.bloquearVuelo(10L)).thenReturn(Optional.of(vuelo));
        lenient().when(boletoRepository.asientosTomadosPorOtras(eq(10L), eq(1L), any(), anyCollection(), any()))
                .thenReturn(List.of());
    }

    @Test
    void seleccionar_adultoYNinoJuntosEnAsientosNormales_noCobraYBloquea() {
        AsientosAsignadosResponse r = service.seleccionar(1L, "tok", request(sel(1, "5A"), sel(2, "5b")));

        assertThat(r.asignaciones()).extracting(AsignacionAsiento::asiento).containsExactly("5A", "5B", null);
        assertThat(r.asignaciones()).extracting(AsignacionAsiento::precioCLP).containsExactly(0L, 0L, 0L);
        assertThat(r.total().totalCLP()).isEqualTo(3 * TARIFA);
        assertThat(r.expiraEn()).isNotNull();
        assertThat(boletoAdulto.getAsientoId().getId()).isEqualTo(id(5, "A"));
        assertThat(boletoAdulto.getExtras()).isEmpty();
        assertThat(reserva.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(3 * TARIFA));
        verify(reservaAcceso).exigirEnCurso(reserva);
        verify(catalogo).bloquearVuelo(10L);
        verify(reservaRepository).save(reserva);
    }

    @Test
    void seleccionar_preferenteEnPackSinSeleccion_cobraElExtraPreferente() {
        AsientosAsignadosResponse r = service.seleccionar(1L, "tok", request(sel(1, "10A")));

        assertThat(r.asignaciones().get(0).precioCLP()).isEqualTo(PREFERENTE);
        assertThat(r.total().totalCLP()).isEqualTo(3 * TARIFA + PREFERENTE);
        assertThat(boletoAdulto.getExtras()).extracting(be -> be.getExtra().getTipo())
                .containsExactly(TipoExtra.ASIENTO_PREFERENTE);
    }

    @Test
    void seleccionar_packConSeleccionIncluida_preferenteTambienEsGratis() {
        pack.setIncluyeSeleccionAsiento(true);

        AsientosAsignadosResponse r = service.seleccionar(1L, "tok", request(sel(1, "10A"), sel(2, "10B")));

        assertThat(r.asignaciones()).extracting(AsignacionAsiento::precioCLP).containsExactly(0L, 0L, 0L);
        assertThat(r.total().totalCLP()).isEqualTo(3 * TARIFA);
        assertThat(boletoAdulto.getExtras()).isEmpty();
    }

    @Test
    void seleccionar_cambiarDeAsiento_reemplazaLosCobrosSinDuplicar() {
        service.seleccionar(1L, "tok", request(sel(1, "10A"), sel(2, "10B")));
        assertThat(boletoNino.getExtras()).hasSize(1);

        AsientosAsignadosResponse r = service.seleccionar(1L, "tok", request(sel(1, "5A"), sel(2, "5B")));

        assertThat(boletoAdulto.getExtras()).isEmpty();
        assertThat(boletoNino.getExtras()).isEmpty();
        assertThat(boletoAdulto.getAsientoId().getFila()).isEqualTo(5);
        assertThat(r.total().totalCLP()).isEqualTo(3 * TARIFA);
    }

    @Test
    void seleccionar_asientoNulo_liberaElAsientoYSuCobro() {
        service.seleccionar(1L, "tok", request(sel(1, "10A")));
        assertThat(boletoAdulto.getExtras()).hasSize(1);

        AsientosAsignadosResponse r = service.seleccionar(1L, "tok", request(sel(1, null)));

        assertThat(boletoAdulto.getAsientoId()).isNull();
        assertThat(boletoAdulto.getExtras()).isEmpty();
        assertThat(r.total().totalCLP()).isEqualTo(3 * TARIFA);
    }

    @Test
    void seleccionar_ninoAntesQueSuAdulto_lanzaAdultoSinAsiento() {
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(2, "5B"))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("ADULTO_SIN_ASIENTO");
        assertThat(boletoNino.getAsientoId()).isNull();
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void seleccionar_ninoAlOtroLadoDelPasillo_lanzaMenorSeparadoDeAdulto() {
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "5C"), sel(2, "5D"))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("MENOR_SEPARADO_DE_ADULTO");
    }

    @Test
    void seleccionar_ninoEnOtraFila_lanzaMenorSeparadoDeAdulto() {
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "5A"), sel(2, "6A"))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("MENOR_SEPARADO_DE_ADULTO");
    }

    @Test
    void seleccionar_adultoSeCambiaYDejaAlNinoAtras_lanzaMenorSeparadoDeAdulto() {
        service.seleccionar(1L, "tok", request(sel(1, "5A"), sel(2, "5B")));

        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "6A"))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("MENOR_SEPARADO_DE_ADULTO");
        assertThat(boletoAdulto.getAsientoId().getId()).isEqualTo(id(5, "A"));
    }

    @Test
    void seleccionar_adultoLiberaSuAsientoConElNinoSentado_lanzaAdultoSinAsiento() {
        service.seleccionar(1L, "tok", request(sel(1, "5A"), sel(2, "5B")));

        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, null))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("ADULTO_SIN_ASIENTO");
    }

    @Test
    void seleccionar_asientoParaBebe_lanzaBebeNoOcupaAsiento() {
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "5A"), sel(3, "5B"))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("BEBE_NO_OCUPA_ASIENTO");
    }

    @Test
    void seleccionar_asientoTomadoPorOtraCompra_lanzaAsientoNoDisponible() {
        when(boletoRepository.asientosTomadosPorOtras(eq(10L), eq(1L), any(), anyCollection(), any()))
                .thenReturn(List.of(id(5, "A")));

        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "5A"))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("ASIENTO_NO_DISPONIBLE");
    }

    @Test
    void seleccionar_asientoDeOtraCabinaOInexistente_lanzaAsientoNoDisponible() {
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "1A"))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("ASIENTO_NO_DISPONIBLE");
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "40Z"))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("ASIENTO_NO_DISPONIBLE");
    }

    @Test
    void seleccionar_mismoAsientoParaDosPasajeros_lanzaAsientoNoDisponible() {
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "5A"), sel(2, "5A"))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("ASIENTO_NO_DISPONIBLE");
    }

    @Test
    void seleccionar_preferenteSinPrecioEnElCatalogo_lanzaAsientoNoDisponibleYElNormalSigueGratis() {
        preferentes.clear();

        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "10A"))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("ASIENTO_NO_DISPONIBLE");
        assertThat(service.seleccionar(1L, "tok", request(sel(1, "5A"))).asignaciones().get(0).precioCLP()).isZero();
    }

    @Test
    void seleccionar_pasajeroOTramoAjeno_lanza404() {
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(new SeleccionarAsientosRequest.Seleccion(99L, 10L, "5A"))))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(catalogo, never()).bloquearVuelo(anyLong());
    }

    @Test
    void seleccionar_sinPasajerosRegistrados_lanzaDatosIncompletos() {
        Pasajero provisorio = CompraReservaFixtures.pendiente(4, reserva, TipoPasajero.ADULTO);
        when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L))
                .thenReturn(List.of(CompraReservaFixtures.boleto(400, reserva, provisorio, tarifa, 1)));

        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(4, "5A"))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("DATOS_INCOMPLETOS");
    }

    @Test
    void seleccionar_compraQueYaNoEstaEnCurso_noTocaNada() {
        doThrow(new ConflictoException("Venció el tiempo para completar la compra").conCodigo(ReservaAcceso.RESERVA_EXPIRADA))
                .when(reservaAcceso).exigirEnCurso(reserva);

        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "5A"))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo(ReservaAcceso.RESERVA_EXPIRADA);
        assertThat(boletoAdulto.getAsientoId()).isNull();
        verify(reservaRepository, never()).save(any());
    }

    /** Id del asiento a partir de su fila y letra, para que no se repita entre filas. */
    private static long id(int fila, String letra) {
        return fila * 10L + (letra.charAt(0) - 'A');
    }

    private static SeleccionarAsientosRequest.Seleccion sel(long pasajeroId, String asiento) {
        return new SeleccionarAsientosRequest.Seleccion(pasajeroId, 10L, asiento);
    }

    private static SeleccionarAsientosRequest request(SeleccionarAsientosRequest.Seleccion... selecciones) {
        return new SeleccionarAsientosRequest(Arrays.asList(selecciones));
    }
}
