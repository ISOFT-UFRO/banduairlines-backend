package com.banduairlines.service.impl;

import com.banduairlines.dto.admin.AsientoResponse;
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
    public List<AsientoResponse> obtenerAsientosPorAeronave(
            Long aeronaveId) {

        return asientoRepository.findByAeronaveId(aeronaveId)
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsientoResponse> obtenerAsientosPorClase(
            Long aeronaveId,
            ClaseCabina claseCabina) {

        return asientoRepository
                .findByAeronaveIdAndClaseCabina(aeronaveId, claseCabina)
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    @Override
    @Transactional
    public AsientoResponse crearAsiento(
            Long aeronaveId,
            Integer fila,
            String letra,
            ClaseCabina claseCabina,
            Boolean esPreferente) {

        Avion avion = avionRepository.findById(aeronaveId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Aeronave no encontrada: " + aeronaveId));

        Asiento asiento = Asiento.builder()
                .aeronave(avion)
                .fila(fila)
                .letra(letra.toUpperCase())
                .claseCabina(claseCabina)
                .esPreferente(esPreferente)
                .build();

        Asiento guardado = asientoRepository.save(asiento);
        return convertirRespuesta(guardado);
    }

    private AsientoResponse convertirRespuesta(Asiento asiento) {
        return new AsientoResponse(
                asiento.getId(),
                asiento.getAeronave().getId(),
                asiento.getFila(),
                asiento.getLetra(),
                asiento.getClaseCabina(),
                asiento.getEsPreferente());
    }
}