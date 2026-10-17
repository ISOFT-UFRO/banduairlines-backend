package com.banduairlines.service.impl;

import com.banduairlines.dto.admin.AsientoRequest;
import com.banduairlines.dto.admin.AvionRequest;
import com.banduairlines.dto.admin.AvionResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Avion;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.Estado;
import com.banduairlines.repository.AsientoRepository;
import com.banduairlines.repository.AvionRepository;
import com.banduairlines.service.impl.AvionServiceImplTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvionServiceImplTest {

    @Mock
    private AvionRepository avionRepository;

    @Mock
    private AsientoRepository asientoRepository;

    @InjectMocks
    private AvionServiceImpl avionService;

    private AvionRequest requestDto;
    private Avion avionGuardado;

    @BeforeEach
    void setUp() {
        AsientoRequest asientoDto = new AsientoRequest();
        asientoDto.setFila(1);
        asientoDto.setLetra("a");
        asientoDto.setClaseCabina(ClaseCabina.BUSINESS);
        asientoDto.setEsPreferente(true);

        requestDto = new AvionRequest();
        requestDto.setMatricula("CC-BNA");
        requestDto.setModelo("Airbus A320neo");
        requestDto.setAsientos(List.of(asientoDto));

        avionGuardado = Avion.builder()
                .id(1L)
                .matricula("CC-BNA")
                .modelo("Airbus A320neo")
                .estado(Estado.ACTIVA)
                .build();
    }

    @Test
    @DisplayName("crearAvion debe lanzar ConflictoException cuando la matrícula ya existe")
    void crearAvion_MatriculaDuplicada_LanzaConflictoException() {
        // Arrange
        when(avionRepository.existsByMatricula(requestDto.getMatricula())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> avionService.crearAvion(requestDto))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Ya existe una aeronave registrada con la matrícula: CC-BNA");

        verify(avionRepository, never()).save(any());
        verify(asientoRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("obtenerPorId debe retornar el DTO de la aeronave cuando el ID existe")
    void obtenerPorId_Exito() {
        // Arrange
        when(avionRepository.findById(1L)).thenReturn(Optional.of(avionGuardado));
        when(asientoRepository.findByAeronaveId(1L)).thenReturn(Collections.emptyList());

        // Act
        AvionResponse respuesta = avionService.obtenerPorId(1L);

        // Assert
        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getId()).isEqualTo(1L);
        assertThat(respuesta.getMatricula()).isEqualTo("CC-BNA");
        assertThat(respuesta.getTotalAsientos()).isEqualTo(0);

        verify(avionRepository, times(1)).findById(1L);
        verify(asientoRepository, times(1)).findByAeronaveId(1L);
    }

    @Test
    @DisplayName("obtenerPorId debe lanzar RecursoNoEncontradoException cuando el ID no existe")
    void obtenerPorId_NoEncontrado_LanzaRecursoNoEncontradoException() {
        // Arrange
        when(avionRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> avionService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Aeronave no encontrada con ID: 99");

        verify(asientoRepository, never()).findByAeronaveId(anyLong());
    }
}