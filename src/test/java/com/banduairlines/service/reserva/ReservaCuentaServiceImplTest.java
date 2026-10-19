package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.security.UsuarioAutenticado;
import com.banduairlines.service.impl.ReservaCuentaServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaCuentaServiceImplTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ReservaConsultaService reservaConsultaService;
    @Mock private UsuarioActual usuarioActual;

    private final CompraTokenService tokens = new CompraTokenService("secreto-de-prueba");
    private ReservaCuentaServiceImpl service;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        service = new ReservaCuentaServiceImpl(reservaRepository, usuarioRepository, tokens, reservaConsultaService, usuarioActual);
        reserva = Reserva.builder().id(1L).codigoReserva("ABC123").estado(EstadoReserva.CONFIRMADA).build();
    }

    @Test
    void asociar_invitadoConSuToken_quedaEnLaCuentaDelPasajero() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        ReservaResponse respuesta = new ReservaResponse(1L, "ABC123", EstadoReserva.CONFIRMADA, null, "Light",
                new ReservaResponse.CantidadPasajeros(1, 0, 0), List.of(), true, List.of(), List.of(), List.of(),
                null, null, null);
        when(usuarioActual.exigir()).thenReturn(pasajero(5L));
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));
        when(usuarioRepository.getReferenceById(5L)).thenReturn(usuario);
        when(reservaConsultaService.armarRespuesta(reserva)).thenReturn(respuesta);

        assertThat(service.asociar(1L, tokens.generar(1L))).isSameAs(respuesta);
        assertThat(reserva.getUsuario()).isSameAs(usuario);
        verify(reservaRepository).save(reserva);
    }

    @Test
    void asociar_reservaYaAsociada_lanzaYaAsociada() {
        reserva.setUsuario(new Usuario());
        when(usuarioActual.exigir()).thenReturn(pasajero(5L));
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> service.asociar(1L, tokens.generar(1L)))
                .isInstanceOfSatisfying(ConflictoException.class, e -> assertThat(e.getCodigo()).isEqualTo("YA_ASOCIADA"));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void asociar_tokenDeOtraReserva_lanza401() {
        when(usuarioActual.exigir()).thenReturn(pasajero(5L));
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> service.asociar(1L, tokens.generar(2L)))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void asociar_administrador_lanzaAccesoDenegado() {
        when(usuarioActual.exigir()).thenReturn(new UsuarioAutenticado(1L, "admin@bandu.cl", RolUsuario.ADMINISTRADOR));

        assertThatThrownBy(() -> service.asociar(1L, tokens.generar(1L))).isInstanceOf(AccessDeniedException.class);
        verify(reservaRepository, never()).findByIdForUpdate(any());
    }

    private static UsuarioAutenticado pasajero(Long id) {
        return new UsuarioAutenticado(id, "tomas@correo.cl", RolUsuario.PASAJERO);
    }
}
