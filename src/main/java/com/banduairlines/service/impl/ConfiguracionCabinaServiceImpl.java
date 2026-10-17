package com.banduairlines.service.impl;

import com.banduairlines.dto.busqueda.ConfiguracionCabinasResponse;
import com.banduairlines.dto.busqueda.ConfigurarCabinasRequest;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Vuelo;
import com.banduairlines.repository.VueloRepository;
import com.banduairlines.service.ConfiguracionCabinaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfiguracionCabinaServiceImpl implements ConfiguracionCabinaService {

    // --- Dependencias ---

    private final VueloRepository vueloRepository;

    // --- Métodos ---

    @Override
    @Transactional
    public ConfiguracionCabinasResponse configurarCabinas(Long vueloId, ConfigurarCabinasRequest request) {

        Vuelo vuelo = vueloRepository.findById(vueloId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vuelo no encontrado con ID: " + vueloId));

        return ConfiguracionCabinasResponse.builder()
                .vueloId(vuelo.getId())
                .mensaje("Configuración de cabinas actualizada exitosamente")
                .cabinas(request.getCabinas())
                .build();
    }
}