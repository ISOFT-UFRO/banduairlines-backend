package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.PaginaReservasResponse;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.impl.ReservaHistorialServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaHistorialServiceImplTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private UsuarioActual usuarioActual;

    @InjectMocks
    private ReservaHistorialServiceImpl service;

    @Test
    void obtenerMisReservas_Exitoso() {
        when(usuarioActual.exigirId()).thenReturn(10L);

        Reserva reserva = new Reserva();
        reserva.setId(100L);
        reserva.setCodigoReserva("MIRES1");
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.setTotal(new BigDecimal("50000"));

        when(reservaRepository.findByUsuario_IdOrderByCreatedAtDescIdDesc(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reserva)));

        PaginaReservasResponse result = service.obtenerMisReservas(null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.elementos().size());
        assertEquals("MIRES1", result.elementos().get(0).codigoReserva());
    }
}
