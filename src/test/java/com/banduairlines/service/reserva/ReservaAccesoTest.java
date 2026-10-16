package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.security.UsuarioAutenticado;

@ExtendWith(MockitoExtension.class)
class ReservaAccesoTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private UsuarioActual usuarioActual;

    private final CompraTokenService tokens = new CompraTokenService("secreto-de-prueba");
    private ReservaAcceso acceso;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(CompraReservaFixtures.AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        acceso = new ReservaAcceso(reservaRepository, usuarioActual, tokens, clock);
    }

    @Test
    void obtenerAutorizada_reservaInexistente_lanza404() {
        when(reservaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acceso.obtenerAutorizada(9L, null)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerAutorizada_invitadoConTokenValido_devuelveLaReserva() {
        Reserva reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        assertThat(acceso.obtenerAutorizada(1L, tokens.generar(1L))).isSameAs(reserva);
    }

    @Test
    void obtenerAutorizada_invitadoSinTokenOTokenDeOtraReserva_lanza401() {
        Reserva reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> acceso.obtenerAutorizada(1L, null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> acceso.obtenerAutorizada(1L, tokens.generar(2L)))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void obtenerAutorizada_reservaDePasajero_exigeSuJwt() {
        Reserva reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        reserva.setUsuario(CompraReservaFixtures.usuario(7L));
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        when(usuarioActual.obtener()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> acceso.obtenerAutorizada(1L, tokens.generar(1L)))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        when(usuarioActual.obtener()).thenReturn(Optional.of(new UsuarioAutenticado(8L, "otro@correo.cl", RolUsuario.PASAJERO)));
        assertThatThrownBy(() -> acceso.obtenerAutorizada(1L, null)).isInstanceOf(AccessDeniedException.class);

        when(usuarioActual.obtener()).thenReturn(Optional.of(new UsuarioAutenticado(7L, "tomas@correo.cl", RolUsuario.PASAJERO)));
        assertThat(acceso.obtenerAutorizada(1L, null)).isSameAs(reserva);
    }

    @Test
    void exigirAbierta_cancelada_lanzaEstadoInvalido() {
        Reserva reserva = CompraReservaFixtures.reserva(EstadoReserva.CANCELADA);

        assertThatThrownBy(() -> acceso.exigirAbierta(reserva))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(ReservaAcceso.ESTADO_INVALIDO));
    }

    @Test
    void exigirAbierta_vencida_lanzaReservaExpirada() {
        Reserva reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        reserva.setExpiraEn(CompraReservaFixtures.AHORA.minusSeconds(1));

        assertThatThrownBy(() -> acceso.exigirAbierta(reserva))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(ReservaAcceso.RESERVA_EXPIRADA));
    }
}
