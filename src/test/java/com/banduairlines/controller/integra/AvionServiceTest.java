package com.banduairlines.controller.integra;

import com.banduairlines.dto.admin.AsientoRequestDto;
import com.banduairlines.dto.admin.AvionRequestDto;
import com.banduairlines.dto.admin.AvionResponseDto;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Asiento;
import com.banduairlines.model.Avion;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.Estado;
import com.banduairlines.repository.AsientoRepository;
import com.banduairlines.repository.AvionRepository;
import com.banduairlines.service.admin.AvionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvionServiceTest {

    @Mock
    private AvionRepository avionRepository;

    @Mock
    private AsientoRepository asientoRepository;

    @InjectMocks
    private AvionService avionService;

    private AvionRequestDto requestDto;
    private Avion avionGuardado;

    @BeforeEach
    void setUp() {
        AsientoRequestDto asientoDto = new AsientoRequestDto();
        asientoDto.setFila(1);
        asientoDto.setLetra("a");
        asientoDto.setClaseCabina(ClaseCabina.BUSINESS);
        asientoDto.setEsPreferente(true);

        requestDto = new AvionRequestDto();
        requestDto.setMatricula("CC-BNA");
        requestDto.setModelo("Airbus A320neo");
        requestDto.setCapacidadCarga(5500.0);
        requestDto.setAsientos(List.of(asientoDto));

        avionGuardado = Avion.builder()
                .id(1L)
                .matricula("CC-BNA")
                .modelo("Airbus A320neo")
                .capacidadCarga(5500.0)
                .estado(Estado.ACTIVA)
                .build();
    }

    @Test
    @DisplayName("crearAvion debe guardar exitosamente una aeronave y sus asientos asociados")
    void crearAvion_Exito() {
        // Arrange
        when(avionRepository.existsByMatricula(requestDto.getMatricula())).thenReturn(false);
        when(avionRepository.save(any(Avion.class))).thenReturn(avionGuardado);
        when(asientoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AvionResponseDto respuesta = avionService.crearAvion(requestDto);

        // Assert
        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getId()).isEqualTo(1L);
        assertThat(respuesta.getMatricula()).isEqualTo("CC-BNA");
        assertThat(respuesta.getCapacidadCarga()).isEqualTo(5500.0);
        assertThat(respuesta.getTotalAsientos()).isEqualTo(1);

        verify(avionRepository, times(1)).existsByMatricula("CC-BNA");
        verify(avionRepository, times(1)).save(any(Avion.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Asiento>> captor = ArgumentCaptor.forClass(List.class);
        verify(asientoRepository, times(1)).saveAll(captor.capture());

        List<Asiento> asientosGuardados = captor.getValue();
        assertThat(asientosGuardados).hasSize(1);
        assertThat(asientosGuardados.get(0).getLetra()).isEqualTo("A"); // Verifica conversión a mayúscula
        assertThat(asientosGuardados.get(0).getClaseCabina()).isEqualTo(ClaseCabina.BUSINESS);
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
        AvionResponseDto respuesta = avionService.obtenerPorId(1L);

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