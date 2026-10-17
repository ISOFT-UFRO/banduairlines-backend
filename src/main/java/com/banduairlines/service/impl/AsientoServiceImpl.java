package com.banduairlines.service.impl;

import com.banduairlines.exception.RecursoNoEncontradoException;

import com.banduairlines.model.Asiento;
import com.banduairlines.model.Avion;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.repository.AsientoRepository;
import com.banduairlines.repository.AvionRepository;
import com.banduairlines.service.admin.AsientoService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AsientoServiceImpl implements AsientoService {

    private final AsientoRepository asientoRepository;
    private final AvionRepository avionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Asiento> obtenerAsientosPorAeronave(Long aeronaveId) {
        return asientoRepository.findByAeronaveId(aeronaveId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asiento> obtenerAsientosPorClase(
            Long aeronaveId,
            ClaseCabina claseCabina) {

        return asientoRepository.findByAeronaveIdAndClaseCabina(
                aeronaveId,
                claseCabina);
    }

    @Override
    @Transactional
    public Asiento crearAsiento(
            Long aeronaveId,
            Integer fila,
            String letra,
            ClaseCabina claseCabina,
            Boolean esPreferente) {

        Avion avion = avionRepository.findById(aeronaveId)
        .orElseThrow(() -> new RecursoNoEncontradoException("Aeronave no encontrada: " + aeronaveId));

        Asiento asiento = Asiento.builder()
                .aeronave(avion)
                .fila(fila)
                .letra(letra.toUpperCase())
                .claseCabina(claseCabina)
                .esPreferente(esPreferente)
                .build();

        return asientoRepository.save(asiento);
    }
}
