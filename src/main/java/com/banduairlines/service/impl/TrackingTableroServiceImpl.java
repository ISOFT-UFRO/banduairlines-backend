package com.banduairlines.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.tracking.ResumenVueloDTO;
import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.service.tracking.RelojOperacional;
import com.banduairlines.service.tracking.TrackingMapper;
import com.banduairlines.service.tracking.TrackingTableroService;

@Service
public class TrackingTableroServiceImpl implements TrackingTableroService {

    private final SeguimientoVueloRepository seguimientoRepository;
    private final RelojOperacional relojOperacional;

    public TrackingTableroServiceImpl(SeguimientoVueloRepository seguimientoRepository,
                                     RelojOperacional relojOperacional) {
        this.seguimientoRepository = seguimientoRepository;
        this.relojOperacional = relojOperacional;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumenVueloDTO> tableroDelDia(LocalDate fecha, EstadoVuelo estado) {
        LocalDate fechaTablero = fecha != null ? fecha : relojOperacional.hoy();
        LocalDateTime desde = fechaTablero.atStartOfDay();
        LocalDateTime hasta = fechaTablero.plusDays(1).atStartOfDay();
        List<SeguimientoVuelo> seguimientos = seguimientoRepository.tablero(desde, hasta, estado);
        return seguimientos.stream()
                .map(TrackingMapper::aResumenDTO)
                .toList();
    }
}