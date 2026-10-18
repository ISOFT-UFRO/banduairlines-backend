package com.banduairlines.service.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.service.tracking.EstadoOperacional;
import com.banduairlines.service.tracking.EstadoOperacionalConsulta;

@Service
public class TrackingEstadoOperacionalConsultaImpl implements EstadoOperacionalConsulta {

    private final SeguimientoVueloRepository seguimientos;

    public TrackingEstadoOperacionalConsultaImpl(SeguimientoVueloRepository seguimientos) {
        this.seguimientos = seguimientos;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, EstadoOperacional> estadosDeVuelos(Collection<Long> vueloIds) {
        if (vueloIds == null) {
            return Map.of();
        }
        var ids = vueloIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return seguimientos.findByVueloIdInOrderBySalidaProgramadaAsc(ids).stream()
                .collect(Collectors.toUnmodifiableMap(
                        SeguimientoVuelo::getVueloId,
                        s -> new EstadoOperacional(s.getEstadoActual(), s.retrasoVigente(), s.getUltimaActualizacion())));
    }
}
