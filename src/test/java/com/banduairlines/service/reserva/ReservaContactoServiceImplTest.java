package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.reserva.ContactoRequest;
import com.banduairlines.dto.reserva.ContactoResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.impl.ReservaContactoServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaContactoServiceImplTest {

    @Mock
    private ReservaAcceso reservaAcceso;
    @Mock
    private ReservaRepository reservaRepository;
    @InjectMocks
    private ReservaContactoServiceImpl service;

    @Test
    void registrar_guardaElCorreoNormalizado() {
        Reserva reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        when(reservaAcceso.obtenerAutorizada(1L, "tok")).thenReturn(reserva);

        ContactoResponse r = service.registrar(1L, "tok", new ContactoRequest("  Tomas@Correo.CL ", " +56 9 1111 2222 "));

        assertThat(r.correo()).isEqualTo("tomas@correo.cl");
        assertThat(r.telefono()).isEqualTo("+56 9 1111 2222");
        assertThat(reserva.getEmailContacto()).isEqualTo("tomas@correo.cl");
        verify(reservaRepository).save(reserva);
    }

    @Test
    void registrar_compraVencida_lanza409() {
        Reserva reserva = CompraReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
        when(reservaAcceso.obtenerAutorizada(1L, "tok")).thenReturn(reserva);
        doThrow(new ConflictoException("x").conCodigo(ReservaAcceso.RESERVA_EXPIRADA)).when(reservaAcceso).exigirEnCurso(reserva);

        assertThatThrownBy(() -> service.registrar(1L, "tok", new ContactoRequest("a@b.cl", "+56911112222")))
                .isInstanceOf(ConflictoException.class);
        verify(reservaRepository, never()).save(any());
    }
}
