package com.banduairlines.service.tracking;

import com.banduairlines.dto.tracking.ActualizarEstadoRequest;
import com.banduairlines.dto.tracking.SeguimientoVueloDTO;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.OrigenEvento;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.impl.TrackingAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingAdminServiceTest {

    @Mock
    private SeguimientoVueloRepository seguimientoRepository;

    @Mock
    private UsuarioActual usuarioActual;

    @Mock
    private RelojOperacional reloj;

    @InjectMocks
    private TrackingAdminServiceImpl trackingAdminService;

    private final LocalDateTime ahoraFijo = LocalDateTime.of(2026, 10, 16, 19, 0);

    @BeforeEach
    void setUp() {
        lenient().when(reloj.ahora()).thenReturn(ahoraFijo);
        lenient().when(usuarioActual.exigirId()).thenReturn(99L);
    }

    @Test
    @DisplayName("Servicio: transicion valida registra evento MANUAL con el id del admin")
    void transicionValida_registraEventoManualConAdminId() {
        Long vueloId = 1L;
        SeguimientoVuelo seguimiento = mock(SeguimientoVuelo.class);
        ActualizarEstadoRequest request = new ActualizarEstadoRequest(
                EstadoVuelo.EMBARCANDO,
                "Puerta 4 habilitada",
                null
        );

        when(seguimientoRepository.buscarConHistorialPorVueloId(vueloId)).thenReturn(Optional.of(seguimiento));

        SeguimientoVueloDTO resultado = trackingAdminService.actualizarEstadoVuelo(vueloId, request);

        assertThat(resultado).isNotNull();
        verify(seguimiento).transicionar(
                EstadoVuelo.EMBARCANDO,
                OrigenEvento.MANUAL,
                "Puerta 4 habilitada",
                99L,
                null,
                ahoraFijo
        );
    }

    @Test
    @DisplayName("Servicio: vuelo inexistente lanza VUELO_NO_ENCONTRADO")
    void vueloInexistente_lanzaRecursoNoEncontrado() {
        Long vueloId = 999L;
        ActualizarEstadoRequest request = new ActualizarEstadoRequest(EstadoVuelo.EMBARCANDO, "Motivo", null);

        when(seguimientoRepository.buscarConHistorialPorVueloId(vueloId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingAdminService.actualizarEstadoVuelo(vueloId, request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("No existe seguimiento para el vuelo: 999");
    }

    @Test
    @DisplayName("Servicio: transicion invalida propaga la excepcion lanzada por transicionar")
    void transicionInvalida_propagaExcepcion() {
        Long vueloId = 1L;
        SeguimientoVuelo seguimiento = mock(SeguimientoVuelo.class);
        ActualizarEstadoRequest request = new ActualizarEstadoRequest(EstadoVuelo.EN_VUELO, null, null);

        when(seguimientoRepository.buscarConHistorialPorVueloId(vueloId)).thenReturn(Optional.of(seguimiento));
        doThrow(new RuntimeException("Transicion no permitida"))
                .when(seguimiento).transicionar(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> trackingAdminService.actualizarEstadoVuelo(vueloId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transicion no permitida");
    }

    @Test
    @DisplayName("Servicio: CANCELADO sin motivo propaga error de motivo requerido")
    void cancelarSinMotivo_propagaError() {
        Long vueloId = 1L;
        SeguimientoVuelo seguimiento = mock(SeguimientoVuelo.class);
        ActualizarEstadoRequest request = new ActualizarEstadoRequest(EstadoVuelo.CANCELADO, null, null);

        when(seguimientoRepository.buscarConHistorialPorVueloId(vueloId)).thenReturn(Optional.of(seguimiento));
        doThrow(new RuntimeException("El motivo es obligatorio para cancelacion"))
                .when(seguimiento).transicionar(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> trackingAdminService.actualizarEstadoVuelo(vueloId, request))
                .isInstanceOf(RuntimeException.class);
    }
}