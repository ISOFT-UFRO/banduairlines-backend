package com.banduairlines.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.tracking.SeguimientoVueloDTO;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.service.tracking.CodigosTracking;
import com.banduairlines.service.tracking.TrackingMapper;
import com.banduairlines.service.tracking.TrackingService;

@Service
public class TrackingServiceImpl implements TrackingService {

    private final SeguimientoVueloRepository seguimientoRepository;

    public TrackingServiceImpl(SeguimientoVueloRepository seguimientoRepository) {
        this.seguimientoRepository = seguimientoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SeguimientoVueloDTO obtenerPorId(Long vueloId) {
        SeguimientoVuelo seguimiento = seguimientoRepository.buscarConHistorialPorVueloId(vueloId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe seguimiento inicializado para el vuelo con ID: " + vueloId)
                        .conCodigo(CodigosTracking.VUELO_NO_ENCONTRADO));

        return TrackingMapper.aSeguimientoDTO(seguimiento);
    }
}
