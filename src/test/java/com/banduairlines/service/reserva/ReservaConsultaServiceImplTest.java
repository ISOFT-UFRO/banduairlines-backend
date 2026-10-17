package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.exception.DemasiadosIntentosException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.reserva.LimitadorIntentosBusqueda;
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
class ReservaConsultaServiceImplTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private LimitadorIntentosBusqueda limitadorIntentos;

    @Mock
    private ReservaAcceso reservaAcceso;

    @InjectMocks
    private ReservaConsultaServiceImpl service;

    @Test
    void buscar_Exitoso() {
        String codigo = "ABC123";
        String correo = "test@bandu.cl";

        Reserva reserva = new Reserva();
        reserva.setCodigoReserva(codigo);
        reserva.setEmailContacto(correo);

        ReservaResponse responseEsperado = mock(ReservaResponse.class);

        when(reservaRepository.findByCodigoReserva(codigo)).thenReturn(Optional.of(reserva));
        when(reservaAcceso.mapear(reserva)).thenReturn(responseEsperado);

        ReservaResponse result = service.buscar(codigo, correo);

        assertNotNull(result);
        verify(limitadorIntentos).exigirIntento();
        verify(limitadorIntentos).registrarExito();
    }

    @Test
    void buscar_CorreoNoCoincide_LanzaRecursoNoEncontrado() {
        String codigo = "ABC123";
        Reserva reserva = new Reserva();
        reserva.setCodigoReserva(codigo);
        reserva.setEmailContacto("otro@bandu.cl");

        when(reservaRepository.findByCodigoReserva(codigo)).thenReturn(Optional.of(reserva));

        assertThrows(RecursoNoEncontradoException.class, () ->
                service.buscar(codigo, "test@bandu.cl")
        );
        verify(limitadorIntentos).registrarFallo();
    }

    @Test
    void buscar_DemasiadosIntentos_LanzaDemasiadosIntentosException() {
        doThrow(new DemasiadosIntentosException("Límite superado"))
                .when(limitadorIntentos).exigirIntento();

        assertThrows(DemasiadosIntentosException.class, () ->
                service.buscar("ABC123", "test@bandu.cl")
        );
    }
}