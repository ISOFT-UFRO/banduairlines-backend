package com.banduairlines.service.impl;

import com.banduairlines.exception.RecursoNoEncontradoException;

import com.banduairlines.model.Asiento;
import com.banduairlines.model.Avion;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.repository.AsientoRepository;
import com.banduairlines.repository.AvionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @BeforeEach
    void setUp() {
        avion = Avion.builder()
                .id(1L)
                .matricula("XA-123")
                .build();

        asiento = Asiento.builder()
                .id(10L)
                .aeronave(avion)
                .fila(1)
                .letra("A")
                .claseCabina(ClaseCabina.BUSINESS) // <-- ajustado
                .esPreferente(true)
                .build();
    }

    @Test
    @DisplayName("Debe retornar lista de asientos por aeronave")
    void obtenerAsientosPorAeronave_retornaLista() {
        when(asientoRepository.findByAeronaveId(1L)).thenReturn(List.of(asiento));

        List<Asiento> resultado = asientoService.obtenerAsientosPorAeronave(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getLetra()).isEqualTo("A");
        verify(asientoRepository).findByAeronaveId(1L);
    }

    @Test
    void obtenerAsientosPorAeronave_listaVacia() {
        when(asientoRepository.findByAeronaveId(99L)).thenReturn(List.of());
        assertThat(asientoService.obtenerAsientosPorAeronave(99L)).isEmpty();
    }

    @Test
    @DisplayName("Debe retornar asientos filtrados por clase")
    void obtenerAsientosPorClase_retornaFiltrados() {
        when(asientoRepository.findByAeronaveIdAndClaseCabina(1L, ClaseCabina.BUSINESS))
                .thenReturn(List.of(asiento));

        List<Asiento> resultado = asientoService
                .obtenerAsientosPorClase(1L, ClaseCabina.BUSINESS);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getClaseCabina()).isEqualTo(ClaseCabina.BUSINESS);
        verify(asientoRepository).findByAeronaveIdAndClaseCabina(1L, ClaseCabina.BUSINESS);
    }

    @Test
    @DisplayName("Debe crear asiento correctamente")
    void crearAsiento_exitoso() {
        when(avionRepository.findById(1L)).thenReturn(Optional.of(avion));
        when(asientoRepository.save(any(Asiento.class))).thenReturn(asiento);

        Asiento resultado = asientoService.crearAsiento(
                1L, 1, "a", ClaseCabina.BUSINESS, true);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getLetra()).isEqualTo("A");
        verify(asientoRepository).save(any(Asiento.class));
    }

    @Test
    @DisplayName("Debe convertir la letra a mayúsculas al crear asiento")
    void crearAsiento_letraMayuscula() {
        when(avionRepository.findById(1L)).thenReturn(Optional.of(avion));
        when(asientoRepository.save(any(Asiento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Asiento resultado = asientoService.crearAsiento(
                1L, 3, "b", ClaseCabina.ECONOMY, false);

        assertThat(resultado.getLetra()).isEqualTo("B");
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la aeronave no existe")
    void crearAsiento_aeronaveNoExiste() {
        when(avionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asientoService.crearAsiento(
                99L, 1, "A", ClaseCabina.PREMIUM_ECONOMY, false))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Aeronave no encontrada: 99");

        verify(asientoRepository, never()).save(any());
    }
}