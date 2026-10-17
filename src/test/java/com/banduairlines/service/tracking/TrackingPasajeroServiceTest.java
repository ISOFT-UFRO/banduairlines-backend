package com.banduairlines.service.tracking;

import static com.banduairlines.model.enums.EstadoVuelo.EMBARCANDO;
import static com.banduairlines.model.enums.EstadoVuelo.RETRASADO;
import static com.banduairlines.model.enums.OrigenEvento.MANUAL;
import static com.banduairlines.model.enums.OrigenEvento.SISTEMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.impl.TrackingPasajeroServiceImpl;
import com.banduairlines.service.tracking.puerto.ReservaExterna;
import com.banduairlines.service.tracking.puerto.ReservaExternaPort;

@ExtendWith(MockitoExtension.class)
class TrackingPasajeroServiceTest {

    private static final LocalDateTime SALIDA = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 20, 8, 0);

    @Mock
    private ReservaExternaPort reservas;

    @Mock
    private SeguimientoVueloRepository seguimientos;

    @Mock
    private UsuarioActual usuarioActual;

    @InjectMocks
    private TrackingPasajeroServiceImpl trackingPasajeroService;

    private static SeguimientoVuelo seguimiento(long vueloId) {
        return SeguimientoVuelo.iniciar(vueloId, "BA-" + vueloId, "SCL", "PMC", SALIDA, SALIDA.plusHours(2), AHORA);
    }

    private static ReservaExterna reserva(EstadoReserva estado) {
        return new ReservaExterna(7L, estado);
    }

    @Test
    @DisplayName("Mis vuelos: entrega estado vigente y retraso del pasajero autenticado")
    void misVuelos_entregaEstadoVigente() {
        SeguimientoVuelo conRetraso = seguimiento(2L);
        conRetraso.transicionar(RETRASADO, MANUAL, "Clima", 9L, 30, AHORA);
        conRetraso.transicionar(EMBARCANDO, SISTEMA, null, null, null, AHORA.plusMinutes(5));

        when(usuarioActual.exigirId()).thenReturn(5L);
        when(reservas.vueloIdsDeUsuario(5L)).thenReturn(List.of(2L));
        when(seguimientos.findByVueloIdInOrderBySalidaProgramadaAsc(List.of(2L))).thenReturn(List.of(conRetraso));

        var vuelos = trackingPasajeroService.consultarMisVuelos();

        assertThat(vuelos).singleElement().satisfies(v -> {
            assertThat(v.vueloId()).isEqualTo(2L);
            assertThat(v.codigoVuelo()).isEqualTo("BA-2");
            assertThat(v.estadoActual()).isEqualTo(EMBARCANDO);
            assertThat(v.retrasoMinutos()).isEqualTo(30);
        });
    }

    @Test
    @DisplayName("Mis vuelos: pasajero sin reservas no consulta seguimientos")
    void misVuelos_sinReservas_noConsultaSeguimientos() {
        when(usuarioActual.exigirId()).thenReturn(5L);
        when(reservas.vueloIdsDeUsuario(5L)).thenReturn(List.of());

        assertThat(trackingPasajeroService.consultarMisVuelos()).isEmpty();
        verify(seguimientos, never()).findByVueloIdInOrderBySalidaProgramadaAsc(anyList());
    }

    @Test
    @DisplayName("Visitante: reserva confirmada con documento valido entrega sus vuelos")
    void visitante_reservaConfirmadaConDocumento_entregaVuelos() {
        when(reservas.buscarPorCodigo("ABC123")).thenReturn(Optional.of(reserva(EstadoReserva.CONFIRMADA)));
        when(reservas.documentoPerteneceAReserva(7L, "12345678-9")).thenReturn(true);
        when(reservas.vueloIdsDeReserva(7L)).thenReturn(List.of(1L));
        when(seguimientos.findByVueloIdInOrderBySalidaProgramadaAsc(List.of(1L)))
                .thenReturn(List.of(seguimiento(1L)));

        var vuelos = trackingPasajeroService.consultarVuelosDeReserva("abc123", "12345678-9");

        assertThat(vuelos).singleElement()
                .extracting("vueloId").isEqualTo(1L);
    }

    @Test
    @DisplayName("Visitante: reserva inexistente lanza RESERVA_NO_ENCONTRADA")
    void visitante_reservaInexistente_lanzaRecursoNoEncontrado() {
        when(reservas.buscarPorCodigo("ZZZ999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingPasajeroService.consultarVuelosDeReserva("ZZZ999", "12345678-9"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("No existe reserva con el código: ZZZ999");
    }

    @Test
    @DisplayName("Visitante: reserva no confirmada lanza RESERVA_NO_CONFIRMADA")
    void visitante_reservaNoConfirmada_lanzaReglaNegocio() {
        when(reservas.buscarPorCodigo("ABC123")).thenReturn(Optional.of(reserva(EstadoReserva.PENDIENTE_PAGO)));

        assertThatThrownBy(() -> trackingPasajeroService.consultarVuelosDeReserva("ABC123", "12345678-9"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Solo es posible consultar los vuelos de una reserva confirmada");
    }

    @Test
    @DisplayName("Visitante: documento que no pertenece a la reserva lanza DOCUMENTO_NO_VALIDO")
    void visitante_documentoNoValido_lanzaReglaNegocio() {
        when(reservas.buscarPorCodigo("ABC123")).thenReturn(Optional.of(reserva(EstadoReserva.CONFIRMADA)));
        when(reservas.documentoPerteneceAReserva(7L, "99999999-9")).thenReturn(false);

        assertThatThrownBy(() -> trackingPasajeroService.consultarVuelosDeReserva("ABC123", "99999999-9"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("El número de documento no corresponde");
    }
}
