package com.banduairlines.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.admin.PackRequest;
import com.banduairlines.dto.admin.PackResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.repository.PackTarifarioRepository;
import com.banduairlines.service.admin.PackService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PackServiceImpl implements PackService {

    private final PackTarifarioRepository packs;

    @Override
    public List<PackResponse> listar(Boolean activo) {
        return packs.listarAdministracion(activo)
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    @Override
    public PackResponse obtener(Long id) {
        return convertirRespuesta(buscarPack(id));
    }

    @Override
    @Transactional
    public PackResponse crear(PackRequest solicitud) {
        validarNombreDisponible(solicitud.nombre(), null);

        PackTarifario pack = PackTarifario.builder()
                .incluyeEquipajeCabina(false)
                .piezasBodegaIncluidas(0)
                .kgPorPiezaBodega(0)
                .activo(true)
                .build();

        asignarDatos(pack, solicitud);

        return convertirRespuesta(packs.saveAndFlush(pack));
    }

    @Override
    @Transactional
    public PackResponse actualizar(Long id, PackRequest solicitud) {
        PackTarifario pack = buscarPack(id);

        validarNombreDisponible(solicitud.nombre(), id);
        asignarDatos(pack, solicitud);

        return convertirRespuesta(packs.saveAndFlush(pack));
    }

    @Override
    @Transactional
    public PackResponse cambiarEstado(Long id, boolean activo) {
        PackTarifario pack = buscarPack(id);
        pack.setActivo(activo);

        return convertirRespuesta(packs.saveAndFlush(pack));
    }

    private PackTarifario buscarPack(Long id) {
        return packs.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pack tarifario", id));
    }

    private void validarNombreDisponible(String nombre, Long excluirId) {
        if (packs.contarDuplicadosAdministracion(nombre, excluirId) > 0) {
            throw new ConflictoException(
                    "Ya existe un pack tarifario con ese nombre")
                    .conCodigo("PACK_DUPLICADO");
        }
    }

    private void asignarDatos(
            PackTarifario pack, PackRequest solicitud) {

        pack.setNombre(solicitud.nombre().trim());
        pack.setDescripcion(solicitud.descripcion());
        pack.setIncluyeSeleccionAsiento(
                solicitud.incluyeSeleccionAsiento());
        pack.setIncluyeEmbarquePrioritario(
                solicitud.incluyeEmbarquePrioritario());
        pack.setPermiteCambios(solicitud.permiteCambios());
        pack.setMultiplicadorMillas(solicitud.multiplicadorMillas());
    }

    private PackResponse convertirRespuesta(PackTarifario pack) {
        return new PackResponse(
                pack.getId(),
                pack.getNombre(),
                pack.getDescripcion(),
                pack.getIncluyeEquipajeCabina(),
                pack.getPiezasBodegaIncluidas(),
                pack.getKgPorPiezaBodega(),
                pack.getIncluyeSeleccionAsiento(),
                pack.getIncluyeEmbarquePrioritario(),
                pack.getPermiteCambios(),
                pack.getMultiplicadorMillas(),
                pack.getActivo());
    }
}