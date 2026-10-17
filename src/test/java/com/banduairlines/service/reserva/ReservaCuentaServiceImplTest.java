package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Usuario;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.reserva.ReservaAcceso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaCuentaServiceImplTest {

    @Mock
    private ReservaAcceso reservaAcceso;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioActual usuarioActual;

    @InjectMocks
    private ReservaCuentaServiceImpl service;

    @Test
    void asociarACuenta_Exitoso() {
        Long reservaId = 1L;
        String token = "compra-token-valido";
        Long usuarioId = 5L;

        Reserva reserva = new Reserva();
        reserva.setId(reservaId);

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        when(reservaAcceso.obtenerAutorizada(reservaId, token)).thenReturn(reserva);
        when(usuarioActual.exigirId()).thenReturn(usuarioId);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(reservaRepository.save(reserva)).thenReturn(reserva);
        when(reservaAcceso.mapear(reserva)).thenReturn(mock(ReservaResponse.class));

        ReservaResponse response = service.asociarACuenta(reservaId, token);

        assertNotNull(response);
        assertEquals(usuario, reserva.getUsuario());
        verify(reservaRepository).save(reserva);
    }

    @Test
    void asociarACuenta_YaAsociada_LanzaConflicto() {
        Long reservaId = 1L;
        String token = "token";

        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setUsuario(new Usuario());

        when(reservaAcceso.obtenerAutorizada(reservaId, token)).thenReturn(reserva);

        assertThrows(ConflictoException.class, () ->
                service.asociarACuenta(reservaId, token)
        );
    }
}
