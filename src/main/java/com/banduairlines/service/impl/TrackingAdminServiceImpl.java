package com.banduairlines.service.impl;

import com.banduairlines.dto.tracking.ActualizarEstadoRequest;
import com.banduairlines.dto.tracking.SeguimientoVueloDTO;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.model.enums.OrigenEvento;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.tracking.TrackingAdminService;
import com.banduairlines.service.tracking.TrackingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class TrackingAdminServiceImpl implements TrackingAdminService {

    private final SeguimientoVueloRepository seguimientoRepository;
    private final UsuarioActual usuarioActual;
    private final Clock clock;

    public TrackingAdminServiceImpl(SeguimientoVueloRepository seguimientoRepository,
                                    UsuarioActual usuarioActual,
                                    Clock clock) {
        this.seguimientoRepository = seguimientoRepository;
        this.usuarioActual = usuarioActual;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SeguimientoVueloDTO actualizarEstadoVuelo(Long vueloId, ActualizarEstadoRequest request) {
        SeguimientoVuelo seguimiento = seguimientoRepository.findByVueloId(vueloId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe seguimiento para el vuelo: " + vueloId)
                        .conCodigo("VUELO_NO_ENCONTRADO"));

        Long adminId = usuarioActual.exigirId();

        seguimiento.transicionar(
                request.nuevoEstado(),
                OrigenEvento.MANUAL,
                request.motivo(),
                adminId,
                request.retrasoMinutos(),
                LocalDateTime.now(clock)
        );

        SeguimientoVuelo guardado = seguimientoRepository.save(seguimiento);
        return TrackingMapper.aSeguimientoDTO(guardado);
    }
}