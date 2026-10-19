package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

import java.time.Clock;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import com.banduairlines.exception.ConflictoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.security.UsuarioActual;

@ExtendWith(MockitoExtension.class)
class ReservaAcceso7a12Test {

    @Mock private ReservaRepository reservaRepository;
    @Mock private UsuarioActual usuarioActual;
    @Mock private CobroReservaService cobroReservaService;
    @Mock private ObjectProvider<CobroReservaService> cobroProvider;

    private ReservaAcceso acceso;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(ReservaFixtures.AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        lenient().when(cobroProvider.getObject()).thenReturn(cobroReservaService);
        acceso = new ReservaAcceso(reservaRepository, usuarioActual, new CompraTokenService("secreto"), clock, cobroProvider);
        reserva = ReservaFixtures.reserva(EstadoReserva.PENDIENTE_PAGO);
    }

    @Test
    void exigirEnCurso_pendientePagoSinCobro_permiteModificar() {
        assertThatCode(() -> acceso.exigirEnCurso(reserva)).doesNotThrowAnyException();
    }

    @Test
    void exigirEnCurso_conCobroIniciado_lanzaEstadoInvalido() {
        lenient().when(cobroReservaService.tieneCobroIniciado(1L)).thenReturn(true);

        assertThatThrownBy(() -> acceso.exigirEnCurso(reserva))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Ya se inició el pago")
                .extracting("codigo").isEqualTo("ESTADO_INVALIDO");
    }

    @Test
    void exigirEnCurso_reservaConfirmada_lanzaEstadoInvalido() {
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        assertThatThrownBy(() -> acceso.exigirEnCurso(reserva))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("ESTADO_INVALIDO");
    }

    @Test
    void exigirEnCurso_vencida_lanzaReservaExpirada() {
        reserva.setExpiraEn(ReservaFixtures.AHORA.minusMinutes(1));

        assertThatThrownBy(() -> acceso.exigirEnCurso(reserva))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("RESERVA_EXPIRADA");
    }
}
