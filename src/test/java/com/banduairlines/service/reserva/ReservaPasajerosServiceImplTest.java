package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.reserva.PasajeroResponse;
import com.banduairlines.dto.reserva.PasajerosRegistradosResponse;
import com.banduairlines.dto.reserva.RegistrarPasajerosRequest;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoDocumento;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.impl.ReservaPasajerosServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaPasajerosServiceImplTest {

    @Mock
    private ReservaAcceso reservaAcceso;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private PasajeroRepository pasajeroRepository;
    @Mock
    private BoletoRepository boletoRepository;

    private ReservaPasajerosServiceImpl service;
    private Reserva reserva;
    private List<Pasajero> pendientes;

    /** Vuelo el 2026-12-10: adulto nacido en 1990, niño en 2019 (7 años), bebé en 2026 (meses). */
    private static final LocalDate ADULTO = LocalDate.of(1990, 5, 1);
    private static final LocalDate NINO = LocalDate.of(2019, 5, 1);
    private static final LocalDate BEBE = LocalDate.of(2026, 3, 1);

    @BeforeEach
    void setUp() {
        service = new ReservaPasajerosServiceImpl(reservaAcceso, reservaRepository, pasajeroRepository,
                boletoRepository, new CalculadoraTotalReserva());
        reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        pendientes = new ArrayList<>(List.of(
                CompraReservaFixtures.pendiente(1, reserva, TipoPasajero.ADULTO),
                CompraReservaFixtures.pendiente(2, reserva, TipoPasajero.ADULTO),
                CompraReservaFixtures.pendiente(3, reserva, TipoPasajero.NINO),
                CompraReservaFixtures.pendiente(4, reserva, TipoPasajero.BEBE)));
        TarifaVuelo tarifa = CompraReservaFixtures.tarifa(CompraReservaFixtures.vuelo(10, CompraReservaFixtures.SALIDA),
                ClaseCabina.ECONOMY, CompraReservaFixtures.packLight(), 50000);
        List<Boleto> boletos = CompraReservaFixtures.boletos(reserva, pendientes, tarifa, 1);

        lenient().when(reservaAcceso.obtenerAutorizada(1L, "tok")).thenReturn(reserva);
        lenient().when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L)).thenReturn(boletos);
        lenient().when(pasajeroRepository.findByReserva_IdOrderByIdAsc(1L)).thenReturn(pendientes);
    }

    private static RegistrarPasajerosRequest.Pasajero p(String ref, TipoPasajero tipo, String nombre, LocalDate nacimiento,
                                                       String doc, String responsable) {
        return new RegistrarPasajerosRequest.Pasajero(ref, tipo, nombre, "Rojas", nacimiento, TipoDocumento.RUT, doc, responsable);
    }

    private static RegistrarPasajerosRequest familia() {
        return new RegistrarPasajerosRequest(List.of(
                p("a1", TipoPasajero.ADULTO, "Tomás", ADULTO, "11.111.111-1", null),
                p("a2", TipoPasajero.ADULTO, "Ana", ADULTO, "22.222.222-2", null),
                p("n1", TipoPasajero.NINO, "Pía", NINO, "33.333.333-3", "a1"),
                p("b1", TipoPasajero.BEBE, "Leo", BEBE, "44.444.444-4", "a2")));
    }

    @Test
    void registrar_reemplazaLosProvisoriosEnLosMismosRegistros() {
        PasajerosRegistradosResponse r = service.registrar(1L, "tok", familia());

        assertThat(r.pasajeros()).extracting(PasajeroResponse::pasajeroId).containsExactly(1L, 2L, 3L, 4L);
        assertThat(r.pasajeros()).extracting(PasajeroResponse::ref).containsExactly("a1", "a2", "n1", "b1");
        assertThat(r.pasajeros().get(2).adultoResponsableId()).isEqualTo(1L);
        assertThat(r.pasajeros().get(3).adultoResponsableId()).isEqualTo(2L);
        assertThat(pendientes).noneMatch(PasajerosPendientes::esPendiente);
        assertThat(pendientes.get(1).getNombre()).isEqualTo("Ana");
        verify(pasajeroRepository).saveAllAndFlush(pendientes);
    }

    @Test
    void registrar_compraNoAbierta_lanza409() {
        doThrow(new ConflictoException("x").conCodigo(ReservaAcceso.RESERVA_EXPIRADA)).when(reservaAcceso).exigirAbierta(reserva);

        assertThatThrownBy(() -> service.registrar(1L, "tok", familia())).isInstanceOf(ConflictoException.class);
        verify(pasajeroRepository, never()).saveAll(any());
    }

    @Test
    void registrar_cantidadDistintaALaDeclarada_lanza422() {
        RegistrarPasajerosRequest request = new RegistrarPasajerosRequest(List.of(
                p("a1", TipoPasajero.ADULTO, "Tomás", ADULTO, "1", null)));

        assertThatThrownBy(() -> service.registrar(1L, "tok", request))
                .isInstanceOfSatisfying(ReglaNegocioException.class, e -> assertThat(e.getCodigo())
                        .isEqualTo("CANTIDAD_PASAJEROS_DISTINTA"));
    }

    @Test
    void registrar_tipoQueNoCoincideConLaEdad_lanza422() {
        RegistrarPasajerosRequest request = new RegistrarPasajerosRequest(List.of(
                p("a1", TipoPasajero.ADULTO, "Tomás", ADULTO, "1", null),
                p("a2", TipoPasajero.ADULTO, "Ana", NINO, "2", null),
                p("n1", TipoPasajero.NINO, "Pía", NINO, "3", "a1"),
                p("b1", TipoPasajero.BEBE, "Leo", BEBE, "4", "a1")));

        assertThatThrownBy(() -> service.registrar(1L, "tok", request))
                .isInstanceOfSatisfying(ReglaNegocioException.class, e -> assertThat(e.getCodigo())
                        .isEqualTo("TIPO_NO_COINCIDE_EDAD"));
    }

    @Test
    void registrar_menorSinAdultoResponsable_lanza422() {
        RegistrarPasajerosRequest request = new RegistrarPasajerosRequest(List.of(
                p("a1", TipoPasajero.ADULTO, "Tomás", ADULTO, "1", null),
                p("a2", TipoPasajero.ADULTO, "Ana", ADULTO, "2", null),
                p("n1", TipoPasajero.NINO, "Pía", NINO, "3", null),
                p("b1", TipoPasajero.BEBE, "Leo", BEBE, "4", "a1")));

        assertThatThrownBy(() -> service.registrar(1L, "tok", request))
                .isInstanceOfSatisfying(ReglaNegocioException.class, e -> assertThat(e.getCodigo())
                        .isEqualTo("MENOR_SIN_ADULTO_RESPONSABLE"));
    }

    @Test
    void registrar_responsableQueNoEsAdulto_lanza422() {
        RegistrarPasajerosRequest request = new RegistrarPasajerosRequest(List.of(
                p("a1", TipoPasajero.ADULTO, "Tomás", ADULTO, "1", null),
                p("a2", TipoPasajero.ADULTO, "Ana", ADULTO, "2", null),
                p("n1", TipoPasajero.NINO, "Pía", NINO, "3", "a1"),
                p("b1", TipoPasajero.BEBE, "Leo", BEBE, "4", "n1")));

        assertThatThrownBy(() -> service.registrar(1L, "tok", request))
                .isInstanceOfSatisfying(ReglaNegocioException.class, e -> assertThat(e.getCodigo())
                        .isEqualTo("MENOR_SIN_ADULTO_RESPONSABLE"));
    }

    @Test
    void tipoSegunEdad_usaLosLimitesDe2Y12Anios() {
        LocalDate vuelo = LocalDate.of(2026, 12, 10);

        assertThat(ReservaPasajerosServiceImpl.tipoSegunEdad(LocalDate.of(2014, 12, 10), vuelo)).isEqualTo(TipoPasajero.ADULTO);
        assertThat(ReservaPasajerosServiceImpl.tipoSegunEdad(LocalDate.of(2014, 12, 11), vuelo)).isEqualTo(TipoPasajero.NINO);
        assertThat(ReservaPasajerosServiceImpl.tipoSegunEdad(LocalDate.of(2024, 12, 10), vuelo)).isEqualTo(TipoPasajero.NINO);
        assertThat(ReservaPasajerosServiceImpl.tipoSegunEdad(LocalDate.of(2024, 12, 11), vuelo)).isEqualTo(TipoPasajero.BEBE);
    }
}
