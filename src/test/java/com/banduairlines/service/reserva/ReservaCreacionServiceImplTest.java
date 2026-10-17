package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.reserva.CrearReservaRequest;
import com.banduairlines.dto.reserva.ReservaCreadaResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.model.enums.Sentido;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.CatalogoReservaRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.security.UsuarioAutenticado;
import com.banduairlines.service.impl.ReservaCreacionServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaCreacionServiceImplTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private PasajeroRepository pasajeroRepository;
    @Mock
    private BoletoRepository boletoRepository;
    @Mock
    private CatalogoReservaRepository catalogoReservaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioActual usuarioActual;
    @Mock
    private ReservaAcceso reservaAcceso;

    @Captor
    private ArgumentCaptor<List<Pasajero>> pasajerosGuardados;
    @Captor
    private ArgumentCaptor<List<Boleto>> boletosGuardados;

    private final CompraTokenService tokens = new CompraTokenService("secreto-de-prueba");
    private ReservaCreacionServiceImpl service;

    private final PackTarifario light = CompraReservaFixtures.packLight();
    private final Vuelo ida = CompraReservaFixtures.vuelo(10, CompraReservaFixtures.SALIDA);
    private final Vuelo vuelta = CompraReservaFixtures.vuelo(11, CompraReservaFixtures.SALIDA.plusDays(5));

    @BeforeEach
    void setUp() {
        service = new ReservaCreacionServiceImpl(reservaRepository, pasajeroRepository, boletoRepository,
                catalogoReservaRepository, usuarioRepository, usuarioActual, tokens, reservaAcceso,
                new PasajerosPendientes(), new CalculadoraTotalReserva(), 15);
        lenient().when(reservaAcceso.ahora()).thenReturn(CompraReservaFixtures.AHORA);
        lenient().when(catalogoReservaRepository.buscarPackPorNombre("light")).thenReturn(Optional.of(light));
        lenient().when(catalogoReservaRepository.bloquearVuelo(10L)).thenReturn(Optional.of(ida));
        lenient().when(catalogoReservaRepository.bloquearVuelo(11L)).thenReturn(Optional.of(vuelta));
        lenient().when(catalogoReservaRepository.buscarTarifa(10L, ClaseCabina.ECONOMY, 1L))
                .thenReturn(Optional.of(CompraReservaFixtures.tarifa(ida, ClaseCabina.ECONOMY, light, 50000)));
        lenient().when(catalogoReservaRepository.buscarTarifa(11L, ClaseCabina.ECONOMY, 1L))
                .thenReturn(Optional.of(CompraReservaFixtures.tarifa(vuelta, ClaseCabina.ECONOMY, light, 40000)));
        lenient().when(catalogoReservaRepository.contarAsientos(5L, ClaseCabina.ECONOMY)).thenReturn(150L);
        lenient().when(boletoRepository.plazasTomadas(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        lenient().when(reservaRepository.existsByCodigoReserva(anyString())).thenReturn(false);
        lenient().when(usuarioActual.obtener()).thenReturn(Optional.empty());
        lenient().when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
    }

    private static CrearReservaRequest request(int adultos, int ninos, int bebes, Long... vuelos) {
        List<CrearReservaRequest.Tramo> tramos = java.util.Arrays.stream(vuelos)
                .map(id -> new CrearReservaRequest.Tramo(id, ClaseCabina.ECONOMY, id == 11L ? Sentido.VUELTA : null))
                .toList();
        return new CrearReservaRequest("ALT-1", tramos, "light", new CrearReservaRequest.CantidadPasajeros(adultos, ninos, bebes));
    }

    @Test
    void crear_invitado_guardaPendientePagoConPasajerosProvisoriosYBoletosPorTramo() {
        ReservaCreadaResponse r = service.crear(request(2, 1, 1, 10L, 11L));

        assertThat(r.reservaId()).isEqualTo(1L);
        assertThat(r.estado()).isEqualTo(EstadoReserva.PENDIENTE_PAGO);
        assertThat(r.codigoReserva()).hasSize(6).matches("[A-Z2-9]{6}");
        assertThat(tokens.esValido(1L, r.compraToken())).isTrue();
        assertThat(r.expiraEn().toLocalDateTime()).isEqualTo(CompraReservaFixtures.AHORA.plusMinutes(15));
        // 4 pasajeros (el bebé paga tarifa completa) x (50.000 + 40.000)
        assertThat(r.total().totalCLP()).isEqualTo(360000);

        verify(pasajeroRepository).saveAll(pasajerosGuardados.capture());
        assertThat(pasajerosGuardados.getValue()).extracting(Pasajero::getTipo)
                .containsExactly(TipoPasajero.ADULTO, TipoPasajero.ADULTO, TipoPasajero.NINO, TipoPasajero.BEBE);
        assertThat(pasajerosGuardados.getValue()).allMatch(PasajerosPendientes::esPendiente);

        verify(boletoRepository).saveAll(boletosGuardados.capture());
        List<Boleto> boletos = boletosGuardados.getValue();
        assertThat(boletos).hasSize(8);
        assertThat(boletos).filteredOn(b -> b.getOrdenTramo() == 2)
                .allMatch(b -> b.getSentido() == Sentido.VUELTA && b.getVueloId() == vuelta);
        assertThat(boletos).filteredOn(b -> b.getOrdenTramo() == 1).allMatch(b -> b.getSentido() == Sentido.IDA);

        ArgumentCaptor<Reserva> reserva = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(reserva.capture());
        assertThat(reserva.getValue().getEmailContacto()).isEmpty();
        assertThat(reserva.getValue().getUsuario()).isNull();
        assertThat(reserva.getValue().getTotal()).isEqualByComparingTo("360000");
    }

    @Test
    void crear_pasajeroRegistrado_quedaAsociadoConSuCorreoYSinToken() {
        when(usuarioActual.obtener()).thenReturn(Optional.of(new UsuarioAutenticado(7L, "tomas@correo.cl", RolUsuario.PASAJERO)));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(CompraReservaFixtures.usuario(7L)));

        ReservaCreadaResponse r = service.crear(request(1, 0, 0, 10L));

        assertThat(r.compraToken()).isNull();
        ArgumentCaptor<Reserva> reserva = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(reserva.capture());
        assertThat(reserva.getValue().getUsuario().getId()).isEqualTo(7L);
        assertThat(reserva.getValue().getEmailContacto()).isEqualTo("tomas@correo.cl");
    }

    @Test
    void crear_masBebesQueAdultos_lanza422SinTocarLaBase() {
        assertThatThrownBy(() -> service.crear(request(1, 0, 2, 10L)))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("BEBES_SIN_ADULTO"));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crear_packInexistente_lanzaSinDisponibilidad() {
        when(catalogoReservaRepository.buscarPackPorNombre("light")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(request(1, 0, 0, 10L)))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("SIN_DISPONIBILIDAD"));
    }

    @Test
    void crear_tramoInexistente_lanza404() {
        assertThatThrownBy(() -> service.crear(request(1, 0, 0, 99L))).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crear_vueloCanceladoOYaSalido_lanzaSinDisponibilidad() {
        ida.setEstado(EstadoVuelo.CANCELADO);
        assertThatThrownBy(() -> service.crear(request(1, 0, 0, 10L))).isInstanceOf(ConflictoException.class);

        ida.setEstado(EstadoVuelo.PROGRAMADO);
        ida.setSalidaProgramada(CompraReservaFixtures.AHORA.minusMinutes(1));
        assertThatThrownBy(() -> service.crear(request(1, 0, 0, 10L))).isInstanceOf(ConflictoException.class);
    }

    @Test
    void crear_sinTarifaParaLaCabina_lanzaSinDisponibilidad() {
        when(catalogoReservaRepository.buscarTarifa(10L, ClaseCabina.ECONOMY, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(request(1, 0, 0, 10L)))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("SIN_DISPONIBILIDAD"));
    }

    @Test
    void crear_sinCupo_lanzaSinDisponibilidad_yLosBebesNoCuentan() {
        when(boletoRepository.plazasTomadas(eq(10L), eq(ClaseCabina.ECONOMY), eq(TipoPasajero.BEBE), any(), any(), any()))
                .thenReturn(149L);

        // 1 adulto + 1 bebe = 1 plaza: cabe
        assertThat(service.crear(request(1, 0, 1, 10L)).reservaId()).isEqualTo(1L);
        // 2 adultos = 2 plazas: no cabe
        assertThatThrownBy(() -> service.crear(request(2, 0, 0, 10L)))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("SIN_DISPONIBILIDAD"));
    }

    @Test
    void crear_codigoRepetido_generaOtro() {
        when(reservaRepository.existsByCodigoReserva(anyString())).thenReturn(true, false);

        assertThat(service.crear(request(1, 0, 0, 10L)).codigoReserva()).hasSize(6);
        verify(reservaRepository, org.mockito.Mockito.times(2)).existsByCodigoReserva(anyString());
    }
}
