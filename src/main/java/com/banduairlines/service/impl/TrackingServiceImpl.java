package com.banduairlines.service.impl;

import java.util.List;

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

    @Override
    @Transactional(readOnly = true)
    public SeguimientoVueloDTO obtenerPorCodigo(String codigoVuelo) {
        List<SeguimientoVuelo> coincidencias = seguimientoRepository.findByCodigoVuelo(codigoVuelo);
        if (coincidencias.isEmpty()) {
            throw new RecursoNoEncontradoException(
                    "No existe seguimiento inicializado para el vuelo con código: " + codigoVuelo)
                    .conCodigo(CodigosTracking.VUELO_NO_ENCONTRADO);
        }

        // El mapper carga el historial dentro de esta transacción de lectura.
        return TrackingMapper.aSeguimientoDTO(coincidencias.getFirst());
    }
}
