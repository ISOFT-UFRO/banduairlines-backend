package com.banduairlines.service.tracking;

import static com.banduairlines.model.enums.EstadoVuelo.EMBARCANDO;
import static com.banduairlines.model.enums.EstadoVuelo.PROGRAMADO;
import static com.banduairlines.model.enums.EstadoVuelo.RETRASADO;
import static com.banduairlines.model.enums.OrigenEvento.MANUAL;
import static com.banduairlines.model.enums.OrigenEvento.SISTEMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.banduairlines.dto.tracking.EventoEstadoDTO;
import com.banduairlines.dto.tracking.SeguimientoVueloDTO;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.service.impl.TrackingServiceImpl;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private SeguimientoVueloRepository seguimientoRepository;

    @InjectMocks
    private TrackingServiceImpl trackingService;

    @Test
    void obtenerPorId_devuelveSeguimientoEHistorialSinEscrituras() {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 16, 9, 0);
        SeguimientoVuelo seguimiento = SeguimientoVuelo.iniciar(42L, "BD0482", "SCL", "ZCO",
                ahora.plusHours(2), ahora.plusHours(3), ahora);
        ReflectionTestUtils.setField(seguimiento, "id", 7L);
        seguimiento.transicionar(RETRASADO, MANUAL, "Clima", 3L, 30, ahora.plusMinutes(10));
        seguimiento.transicionar(EMBARCANDO, SISTEMA, null, null, null, ahora.plusMinutes(20));
        when(seguimientoRepository.buscarConHistorialPorVueloId(42L)).thenReturn(Optional.of(seguimiento));

        SeguimientoVueloDTO respuesta = trackingService.obtenerPorId(42L);

        assertThat(respuesta.idSeguimiento()).isEqualTo(7L);
        assertThat(respuesta.vueloId()).isEqualTo(42L);
        assertThat(respuesta.codigoVuelo()).isEqualTo("BD0482");
        assertThat(respuesta.origenIata()).isEqualTo("SCL");
        assertThat(respuesta.destinoIata()).isEqualTo("ZCO");
        assertThat(respuesta.salidaProgramada()).isEqualTo(ahora.plusHours(2));
        assertThat(respuesta.llegadaProgramada()).isEqualTo(ahora.plusHours(3));
        assertThat(respuesta.estadoActual()).isEqualTo(EMBARCANDO);
        assertThat(respuesta.retrasoMinutos()).isEqualTo(30);
        assertThat(respuesta.historial()).extracting(EventoEstadoDTO::estadoNuevo)
                .containsExactly(EMBARCANDO, RETRASADO, PROGRAMADO);
        assertThat(respuesta.historial().get(1).estadoAnterior()).isEqualTo(PROGRAMADO);
        assertThat(respuesta.historial().get(1).origen()).isEqualTo(MANUAL);
        assertThat(respuesta.historial().get(1).motivo()).isEqualTo("Clima");
        assertThat(respuesta.historial().get(2).estadoAnterior()).isNull();
        assertThat(seguimiento.getEventos()).hasSize(3);
        verify(seguimientoRepository).buscarConHistorialPorVueloId(42L);
        verifyNoMoreInteractions(seguimientoRepository);
    }

    @Test
    void obtenerPorId_sinSeguimiento_lanza404ConCodigoDeTracking() {
        when(seguimientoRepository.buscarConHistorialPorVueloId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingService.obtenerPorId(99L))
                .isInstanceOfSatisfying(RecursoNoEncontradoException.class,
                        error -> assertThat(error.getCodigo()).isEqualTo(CodigosTracking.VUELO_NO_ENCONTRADO));

        verify(seguimientoRepository).buscarConHistorialPorVueloId(99L);
        verifyNoMoreInteractions(seguimientoRepository);
    }
}
