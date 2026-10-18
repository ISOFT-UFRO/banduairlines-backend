package com.banduairlines.service.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.impl.TrackingPasajeroServiceImpl;
import com.banduairlines.service.tracking.puerto.VuelosDelUsuarioPort;

@ExtendWith(MockitoExtension.class)
class TrackingPasajeroServiceTest {

    private static final LocalDateTime SALIDA = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 20, 8, 0);

    @Mock
    private VuelosDelUsuarioPort vuelosDelUsuario;

    @Mock
    private SeguimientoVueloRepository seguimientos;

    @Mock
    private UsuarioActual usuarioActual;

    @InjectMocks
    private TrackingPasajeroServiceImpl trackingPasajeroService;

    private static SeguimientoVuelo seguimiento(long vueloId) {
        return SeguimientoVuelo.iniciar(vueloId, "BA-" + vueloId, "SCL", "PMC", SALIDA, SALIDA.plusHours(2), AHORA);
    }

    @Test
    @DisplayName("lista vacía devuelve [] sin consultar seguimientos")
    void misVuelos_sinVuelos_noConsultaSeguimientos() {
        when(usuarioActual.exigirId()).thenReturn(5L);
        when(vuelosDelUsuario.vueloIdsDeUsuario(5L)).thenReturn(List.of());

        assertThat(trackingPasajeroService.consultarMisVuelos()).isEmpty();
        verify(seguimientos, never()).findByVueloIdInOrderBySalidaProgramadaAsc(anyList());
    }

    @Test
    @DisplayName("vuelos sin seguimiento no aparecen")
    void misVuelos_vueloSinSeguimiento_noAparece() {
        when(usuarioActual.exigirId()).thenReturn(5L);
        when(vuelosDelUsuario.vueloIdsDeUsuario(5L)).thenReturn(List.of(1L, 2L));
        when(seguimientos.findByVueloIdInOrderBySalidaProgramadaAsc(List.of(1L, 2L)))
                .thenReturn(List.of(seguimiento(1L)));

        var vuelos = trackingPasajeroService.consultarMisVuelos();

        assertThat(vuelos).singleElement().satisfies(v -> {
            assertThat(v.vueloId()).isEqualTo(1L);
            assertThat(v.codigoVuelo()).isEqualTo("BA-1");
        });
    }
}
