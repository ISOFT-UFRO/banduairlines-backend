package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.banduairlines.dto.reserva.PaginaReservasResponse;
import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.exception.DemasiadosIntentosException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.impl.ReservaHistorialServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaHistorialServiceImplTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private ReservaConsultaService reservaConsultaService;
    @Mock private LimitadorIntentosBusqueda limitador;
    @Mock private UsuarioActual usuarioActual;

    private ReservaHistorialServiceImpl service;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        service = new ReservaHistorialServiceImpl(reservaRepository, reservaConsultaService, limitador, usuarioActual);
        reserva = Reserva.builder().id(100L).codigoReserva("MIRES1").emailContacto("tomas@correo.cl")
                .estado(EstadoReserva.CONFIRMADA).total(new BigDecimal("50000")).build();
    }

    @Test
    void listarMias_devuelveLasReservasDelPasajero() {
        when(usuarioActual.exigirId()).thenReturn(10L);
        when(reservaRepository.findByUsuario_IdOrderByCreatedAtDescIdDesc(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reserva)));
        when(reservaConsultaService.armarRespuesta(reserva)).thenReturn(respuesta("MIRES1"));

        PaginaReservasResponse pagina = service.listarMias(null, 0, 10);

        assertThat(pagina.contenido()).hasSize(1);
        assertThat(pagina.contenido().get(0).codigoReserva()).isEqualTo("MIRES1");
        assertThat(pagina.tamano()).isEqualTo(10);
    }

    @Test
    void buscar_codigoYCorreoCoinciden_devuelveLaReserva() {
        ReservaResponse esperada = respuesta("MIRES1");
        when(reservaRepository.findByCodigoReserva("MIRES1")).thenReturn(Optional.of(reserva));
        when(reservaConsultaService.armarRespuesta(reserva)).thenReturn(esperada);

        assertThat(service.buscar(" mires1 ", "Tomas@Correo.cl", "10.0.0.1")).isSameAs(esperada);
        verify(limitador).exigirCupo("10.0.0.1", "MIRES1", "tomas@correo.cl");
        verify(limitador).registrarIntento("10.0.0.1");
        verify(limitador, never()).registrarFallo(anyString(), anyString());
    }

    @Test
    void buscar_correoNoCoincide_lanza404YRegistraElFallo() {
        when(reservaRepository.findByCodigoReserva("MIRES1")).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> service.buscar("MIRES1", "otro@correo.cl", "10.0.0.1"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(limitador).registrarFallo("MIRES1", "otro@correo.cl");
    }

    @Test
    void buscar_sinCupo_lanzaDemasiadosIntentosSinConsultar() {
        doThrow(new DemasiadosIntentosException(600)).when(limitador).exigirCupo("10.0.0.1", "MIRES1", "tomas@correo.cl");

        assertThatThrownBy(() -> service.buscar("MIRES1", "tomas@correo.cl", "10.0.0.1"))
                .isInstanceOf(DemasiadosIntentosException.class);
        verify(reservaRepository, never()).findByCodigoReserva(anyString());
    }

    private static ReservaResponse respuesta(String codigo) {
        return new ReservaResponse(100L, codigo, EstadoReserva.CONFIRMADA, null, "Light",
                new ReservaResponse.CantidadPasajeros(1, 0, 0), List.of(), true, List.of(), List.of(), List.of(),
                null, null, null);
    }
}
