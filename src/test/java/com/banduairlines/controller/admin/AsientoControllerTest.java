package com.banduairlines.controller.admin;

import com.banduairlines.model.Asiento;
import com.banduairlines.model.Avion;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.service.AsientoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsientoControllerTest {

    @Mock
    private AsientoService asientoService;

    @InjectMocks
    private AsientoController asientoController;

    private Asiento asientoEjemplo;

    @BeforeEach
    void setUp() {
        Avion avion = Avion.builder().id(1L).matricula("XA-123").build();
        asientoEjemplo = Asiento.builder()
                .id(10L)
                .aeronave(avion)
                .fila(1)
                .letra("A")
                .claseCabina(ClaseCabina.BUSINESS)
                .esPreferente(true)
                .build();
    }

    // ---------- GET: listarPorAeronave ----------

    @Test
    @DisplayName("GET sin claseCabina delega a obtenerAsientosPorAeronave")
    void listarPorAeronave_sinClase() {
        when(asientoService.obtenerAsientosPorAeronave(1L))
                .thenReturn(List.of(asientoEjemplo));

        ResponseEntity<List<Asiento>> respuesta =
                asientoController.listarPorAeronave(1L, null);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
        assertThat(respuesta.getBody().get(0).getLetra()).isEqualTo("A");

        verify(asientoService).obtenerAsientosPorAeronave(1L);
        verify(asientoService, never()).obtenerAsientosPorClase(anyLong(), any());
    }

    @Test
    @DisplayName("GET con claseCabina delega a obtenerAsientosPorClase")
    void listarPorAeronave_conClase() {
        when(asientoService.obtenerAsientosPorClase(1L, ClaseCabina.BUSINESS))
                .thenReturn(List.of(asientoEjemplo));

        ResponseEntity<List<Asiento>> respuesta =
                asientoController.listarPorAeronave(1L, ClaseCabina.BUSINESS);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
        assertThat(respuesta.getBody().get(0).getClaseCabina())
                .isEqualTo(ClaseCabina.BUSINESS);

        verify(asientoService).obtenerAsientosPorClase(1L, ClaseCabina.BUSINESS);
        verify(asientoService, never()).obtenerAsientosPorAeronave(anyLong());
    }

    @Test
    @DisplayName("GET retorna lista vacía cuando no hay asientos")
    void listarPorAeronave_listaVacia() {
        when(asientoService.obtenerAsientosPorAeronave(99L)).thenReturn(List.of());

        ResponseEntity<List<Asiento>> respuesta =
                asientoController.listarPorAeronave(99L, null);

        assertThat(respuesta.getBody()).isEmpty();
    }

    // ---------- POST: crearAsiento ----------

    @Test
    @DisplayName("POST retorna 201 CREATED con el asiento creado")
    void crearAsiento_retorna201() {
        when(asientoService.crearAsiento(1L, 1, "A", ClaseCabina.BUSINESS, true))
                .thenReturn(asientoEjemplo);

        ResponseEntity<Asiento> respuesta = asientoController.crearAsiento(
                1L, 1, "A", ClaseCabina.BUSINESS, true);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getLetra()).isEqualTo("A");
        assertThat(respuesta.getBody().getEsPreferente()).isTrue();

        verify(asientoService).crearAsiento(1L, 1, "A", ClaseCabina.BUSINESS, true);
    }

    @Test
    @DisplayName("POST propaga la excepción si el servicio falla")
    void crearAsiento_servicioFalla() {
        when(asientoService.crearAsiento(anyLong(), anyInt(), anyString(), any(), anyBoolean()))
                .thenThrow(new RuntimeException("Aeronave no encontrada"));

        try {
            asientoController.crearAsiento(99L, 1, "A", ClaseCabina.BUSINESS, false);
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage()).contains("Aeronave no encontrada");
        }

        verify(asientoService).crearAsiento(99L, 1, "A", ClaseCabina.BUSINESS, false);
    }
}