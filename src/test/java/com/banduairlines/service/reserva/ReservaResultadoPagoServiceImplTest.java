package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.ResultadoPagoRequest;
import com.banduairlines.dto.reserva.ResultadoPagoResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.impl.ReservaResultadoPagoServiceImpl;
import com.banduairlines.service.reserva.ClaveServicioInterna;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaResultadoPagoServiceImplTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private BoletoRepository boletoRepository;

    @Mock
    private ClaveServicioInterna claveServicioInterna;

    @InjectMocks
    private ReservaResultadoPagoServiceImpl service;

    private Reserva reserva;
    private ResultadoPagoRequest requestValida;

    @BeforeEach
    void setUp() {
        reserva = new Reserva();
        reserva.setId(1L);
        reserva.setTotal(new BigDecimal("150000"));
        reserva.setEstado(EstadoReserva.PENDIENTE_PAGO);

        requestValida = new ResultadoPagoRequest(
                "PAGO-123",
                "TX-999",
                "PAGADO",
                150000L,
                LocalDateTime.now(),
                null
        );
    }

    @Test
    void cuandoPagoEsExitoso_debeConfirmarReservaYAsientos() {
        doNothing().when(claveServicioInterna).exigirCoincidencia("key-secret");
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        
        Boleto boleto = new Boleto();
        when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L)).thenReturn(List.of(boleto));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        ResultadoPagoResponse response = service.registrarResultadoPago(1L, requestValida, "key-secret");

        assertNotNull(response);
        assertEquals(EstadoReserva.CONFIRMADA, response.estado());
        assertTrue(boleto.isAsientoConfirmado());
        verify(reservaRepository).save(reserva);
    }

    @Test
    void cuandoMontoNoCoincide_debeLanzarConflictoException() {
        doNothing().when(claveServicioInterna).exigirCoincidencia("key-secret");
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        ResultadoPagoRequest requestMontoIncorrecto = new ResultadoPagoRequest(
                "PAGO-123", "TX-999", "PAGADO", 100000L, LocalDateTime.now(), null
        );

        assertThrows(ConflictoException.class, () ->
                service.registrarResultadoPago(1L, requestMontoIncorrecto, "key-secret")
        );
    }

    @Test
    void cuandoReservaNoExiste_debeLanzarRecursoNoEncontradoException() {
        doNothing().when(claveServicioInterna).exigirCoincidencia("key-secret");
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () ->
                service.registrarResultadoPago(99L, requestValida, "key-secret")
        );
    }
}