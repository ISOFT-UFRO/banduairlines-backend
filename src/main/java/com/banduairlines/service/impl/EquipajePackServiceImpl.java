package com.banduairlines.service.impl;

import com.banduairlines.dto.admin.EquipajePackRespuesta;
import com.banduairlines.dto.admin.EquipajePackSolicitud;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.repository.PackTarifarioRepository;
import com.banduairlines.service.admin.EquipajePackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipajePackServiceImpl implements EquipajePackService {

    private final PackTarifarioRepository packs;

    @Override
    public EquipajePackRespuesta obtener(Long packId) {
        return convertirRespuesta(buscarPack(packId));
    }

    @Override
    @Transactional
    public EquipajePackRespuesta actualizar(
            Long packId, EquipajePackSolicitud solicitud) {

        PackTarifario pack = buscarPack(packId);
        validarEquipaje(solicitud);

        pack.setIncluyeEquipajeCabina(
                solicitud.incluyeEquipajeCabina());
        pack.setPiezasBodegaIncluidas(
                solicitud.piezasBodegaIncluidas());
        pack.setKgPorPiezaBodega(
                solicitud.kgPorPiezaBodega());

        return convertirRespuesta(packs.saveAndFlush(pack));
    }

    private void validarEquipaje(EquipajePackSolicitud solicitud) {
        int piezas = solicitud.piezasBodegaIncluidas();
        int kilos = solicitud.kgPorPiezaBodega();

        if (piezas == 0 && kilos != 0) {
            throw new ReglaNegocioException(
                    "Si el pack no incluye piezas de bodega, "
                            + "el peso por pieza debe ser cero")
                    .conCodigo("EQUIPAJE_BODEGA_INCOHERENTE");
        }

        if (piezas > 0 && kilos == 0) {
            throw new ReglaNegocioException(
                    "Si el pack incluye piezas de bodega, "
                            + "el peso por pieza debe ser mayor que cero")
                    .conCodigo("EQUIPAJE_BODEGA_INCOHERENTE");
        }
    }

    private PackTarifario buscarPack(Long packId) {
        return packs.findById(packId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Pack tarifario",
                        packId));
    }

    private EquipajePackRespuesta convertirRespuesta(
            PackTarifario pack) {

        return new EquipajePackRespuesta(
                pack.getId(),
                pack.getNombre(),
                pack.getIncluyeEquipajeCabina(),
                pack.getPiezasBodegaIncluidas(),
                pack.getKgPorPiezaBodega());
    }
}