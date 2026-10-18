package com.banduairlines.service.impl;

import com.banduairlines.dto.admin.AsientoResponse;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Asiento;
import com.banduairlines.model.Avion;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.repository.AsientoRepository;
import com.banduairlines.repository.AvionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsientoServiceImplTest {

    @Mock
    private AsientoRepository asientoRepository;

    @Mock
    private AvionRepository avionRepository;

    @InjectMocks
    private AsientoServiceImpl asientoService;

    private Avion avion;
    private Asiento asiento;
    private AsientoResponse respuestaEsperada;

    @BeforeEach
    void preparar() {
        avion = Avion.builder()
                .id(1L)
                .matricula("XA-123")
                .build();

        asiento = Asiento.builder()
                .id(10L)
                .aeronave(avion)
                .fila(1)
                .letra("A")
                .claseCabina(ClaseCabina.BUSINESS)
                .esPreferente(true)
                .build();

        respuestaEsperada = new AsientoResponse(
                10L, 1L, 1, "A", ClaseCabina.BUSINESS, true);
    }

    @Test
    void obtenerAsientosPorAeronave_retornaLista() {
        when(asientoRepository.findByAeronaveId(1L))
                .thenReturn(List.of(asiento));

        List<AsientoResponse> resultado = asientoService.obtenerAsientosPorAeronave(1L);

        assertThat(resultado).containsExactly(respuestaEsperada);
        verify(asientoRepository).findByAeronaveId(1L);
    }

    @Test
    void obtenerAsientosPorAeronave_listaVacia() {
        when(asientoRepository.findByAeronaveId(99L))
                .thenReturn(List.of());

        assertThat(asientoService.obtenerAsientosPorAeronave(99L))
                .isEmpty();
    }

    @Test
    void obtenerAsientosPorClase_retornaFiltrados() {
        when(asientoRepository.findByAeronaveIdAndClaseCabina(
                1L, ClaseCabina.BUSINESS))
                .thenReturn(List.of(asiento));

        List<AsientoResponse> resultado = asientoService.obtenerAsientosPorClase(
                1L, ClaseCabina.BUSINESS);

        assertThat(resultado).containsExactly(respuestaEsperada);

        verify(asientoRepository).findByAeronaveIdAndClaseCabina(
                1L, ClaseCabina.BUSINESS);
    }

    @Test
    void crearAsiento_exitoso() {
        when(avionRepository.findById(1L))
                .thenReturn(Optional.of(avion));

        when(asientoRepository.save(any(Asiento.class)))
                .thenAnswer(invocacion -> {
                    Asiento guardado = invocacion.getArgument(0);
                    guardado.setId(10L);
                    return guardado;
                });

        AsientoResponse resultado = asientoService.crearAsiento(
                1L, 1, "a", ClaseCabina.BUSINESS, true);

        assertThat(resultado).isEqualTo(respuestaEsperada);
        verify(asientoRepository).save(any(Asiento.class));
    }

    @Test
    void crearAsiento_letraMayuscula() {
        when(avionRepository.findById(1L))
                .thenReturn(Optional.of(avion));

        when(asientoRepository.save(any(Asiento.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        AsientoResponse resultado = asientoService.crearAsiento(
                1L, 3, "b", ClaseCabina.ECONOMY, false);

        assertThat(resultado.letra()).isEqualTo("B");
        assertThat(resultado.aeronaveId()).isEqualTo(1L);
    }

    @Test
    void crearAsiento_aeronaveNoExiste() {
        when(avionRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> asientoService.crearAsiento(
                99L, 1, "A", ClaseCabina.PREMIUM_ECONOMY, false))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Aeronave no encontrada: 99");

        verifyNoInteractions(asientoRepository);
    }
}