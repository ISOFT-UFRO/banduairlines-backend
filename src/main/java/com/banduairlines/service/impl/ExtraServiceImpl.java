package com.banduairlines.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.admin.ExtraResponse;
import com.banduairlines.dto.admin.ExtraRequest;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Extra;
import com.banduairlines.repository.ExtraRepository;
import com.banduairlines.service.admin.ExtraService;

@Service
@Transactional
public class ExtraServiceImpl implements ExtraService {

    private final ExtraRepository extraRepository;

    public ExtraServiceImpl(ExtraRepository extraRepository) {
        this.extraRepository = extraRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExtraResponse> listar(Boolean activo) {
        return extraRepository.listarAdministracion(activo)
                .stream()
                .map(this::mapearADto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExtraResponse obtener(Long id) {
        return mapearADto(exigirExtra(id));
    }

    @Override
    public ExtraResponse crear(ExtraRequest solicitud) {
        Extra extra = Extra.builder()
                .tipo(solicitud.tipo())
                .nombre(solicitud.nombre())
                .pesoKg(solicitud.pesoKg())
                .precio(solicitud.precio())
                .activo(true)
                .build();

        return mapearADto(extraRepository.save(extra));
    }

    @Override
    public ExtraResponse actualizar(Long id, ExtraRequest solicitud) {
        Extra extra = exigirExtra(id);

        extra.setTipo(solicitud.tipo());
        extra.setNombre(solicitud.nombre());
        extra.setPesoKg(solicitud.pesoKg());
        extra.setPrecio(solicitud.precio());

        return mapearADto(extra);
    }

    @Override
    public ExtraResponse cambiarEstado(Long id, boolean activo) {
        Extra extra = exigirExtra(id);
        extra.setActivo(activo);
        return mapearADto(extra);
    }

    private Extra exigirExtra(Long id) {
        return extraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Extra", id));
    }

    private ExtraResponse mapearADto(Extra extra) {
        return new ExtraResponse(
                extra.getId(),
                extra.getTipo(),
                extra.getNombre(),
                extra.getPesoKg(),
                extra.getPrecio(),
                extra.getActivo()
        );
    }
}
