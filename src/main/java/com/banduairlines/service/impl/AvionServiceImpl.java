package com.banduairlines.service.impl;

import com.banduairlines.dto.admin.AvionRequestDto;
import com.banduairlines.dto.admin.AvionResponseDto;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Asiento;
import com.banduairlines.model.Avion;
import com.banduairlines.model.enums.Estado;
import com.banduairlines.repository.AsientoRepository;
import com.banduairlines.repository.AvionRepository;
import com.banduairlines.service.admin.AvionService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvionServiceImpl implements AvionService {

    private final AvionRepository avionRepository;
    private final AsientoRepository asientoRepository;

    @Transactional
    @Override
    public AvionResponseDto crearAvion(AvionRequestDto request) {
        if (avionRepository.existsByMatricula(request.getMatricula())) {
            throw new ConflictoException("Ya existe una aeronave registrada con la matrícula: " + request.getMatricula());
        }

        Avion avion = Avion.builder()
                .matricula(request.getMatricula())
                .modelo(request.getModelo())
                .estado(Estado.ACTIVA)
                .build();

        Avion avionGuardado = avionRepository.save(avion);

        int totalAsientos = 0;
        if (request.getAsientos() != null && !request.getAsientos().isEmpty()) {
            List<Asiento> asientos = request.getAsientos().stream().map(aDto ->
                    Asiento.builder()
                            .aeronave(avionGuardado)
                            .fila(aDto.getFila())
                            .letra(aDto.getLetra().toUpperCase())
                            .claseCabina(aDto.getClaseCabina())
                            .esPreferente(aDto.getEsPreferente())
                            .build()
            ).collect(Collectors.toList());

            asientoRepository.saveAll(asientos);
            totalAsientos = asientos.size();
        }

        return AvionResponseDto.builder()
                .id(avionGuardado.getId())
                .matricula(avionGuardado.getMatricula())
                .modelo(avionGuardado.getModelo())
                .estado(avionGuardado.getEstado())
                .totalAsientos(totalAsientos)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public AvionResponseDto obtenerPorId(Long id) {
        Avion avion = avionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Aeronave no encontrada con ID: " + id));

        int totalAsientos = asientoRepository.findByAeronaveId(id).size();

        return AvionResponseDto.builder()
                .id(avion.getId())
                .matricula(avion.getMatricula())
                .modelo(avion.getModelo())
                .estado(avion.getEstado())
                .totalAsientos(totalAsientos)
                .build();
    }
}