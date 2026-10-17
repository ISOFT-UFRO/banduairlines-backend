package com.banduairlines.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.admin.TarifaRequest;
import com.banduairlines.dto.admin.TarifaResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.repository.AsientoRepository;
import com.banduairlines.repository.PackTarifarioRepository;
import com.banduairlines.repository.TarifaVueloRepository;
import com.banduairlines.repository.VueloRepository;
import com.banduairlines.service.admin.TarifaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TarifaServiceImpl implements TarifaService {

    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999");

    private final TarifaVueloRepository tarifas;
    private final VueloRepository vuelos;
    private final PackTarifarioRepository packs;
    private final AsientoRepository asientos;

    @Override
    public List<TarifaResponse> listar(
            Long vueloId, ClaseCabina claseCabina) {

        buscarVuelo(vueloId);

        return tarifas.listarAdministracion(vueloId, claseCabina)
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    @Override
    public TarifaResponse obtener(Long vueloId, Long id) {
        buscarVuelo(vueloId);
        return convertirRespuesta(buscarTarifa(vueloId, id));
    }

    @Override
    @Transactional
    public TarifaResponse crear(
            Long vueloId, TarifaRequest solicitud) {

        Vuelo vuelo = buscarVuelo(vueloId);
        PackTarifario pack = buscarPack(solicitud.packId());

        if (!Boolean.TRUE.equals(pack.getActivo())) {
            throw new ReglaNegocioException(
                    "No se puede crear una tarifa para un pack inactivo")
                    .conCodigo("PACK_INACTIVO");
        }

        validarPrecio(solicitud.precio());
        validarCabina(vuelo, solicitud.claseCabina());

        if (tarifas.contarDuplicadasAdministracion(
                vueloId, solicitud.claseCabina(), solicitud.packId()) > 0) {

            throw new ConflictoException(
                    "Ya existe una tarifa para ese vuelo, cabina y pack")
                    .conCodigo("TARIFA_DUPLICADA");
        }

        TarifaVuelo tarifa = TarifaVuelo.builder()
                .vuelo(vuelo)
                .claseCabina(solicitud.claseCabina())
                .pack(pack)
                .precio(solicitud.precio())
                .build();

        return convertirRespuesta(tarifas.saveAndFlush(tarifa));
    }

    @Override
    @Transactional
    public TarifaResponse actualizarPrecio(
            Long vueloId, Long id, BigDecimal precio) {

        Vuelo vuelo = buscarVuelo(vueloId);
        TarifaVuelo tarifa = buscarTarifa(vueloId, id);

        validarPrecio(precio);
        validarCabina(vuelo, tarifa.getClaseCabina());

        tarifa.setPrecio(precio);

        return convertirRespuesta(tarifas.saveAndFlush(tarifa));
    }

    private Vuelo buscarVuelo(Long id) {
        return vuelos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vuelo", id));
    }

    private PackTarifario buscarPack(Long id) {
        return packs.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pack tarifario", id));
    }

    private TarifaVuelo buscarTarifa(Long vueloId, Long id) {
        return tarifas.buscarAdministracion(vueloId, id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa", id));
    }

    private void validarCabina(Vuelo vuelo, ClaseCabina claseCabina) {
        if (claseCabina == null
                || asientos.findByAeronaveIdAndClaseCabina(
                        vuelo.getAeronave().getId(), claseCabina).isEmpty()) {

            throw new ReglaNegocioException(
                    "La aeronave del vuelo no tiene asientos de esa cabina")
                    .conCodigo("CABINA_NO_DISPONIBLE_EN_AERONAVE");
        }
    }

    private void validarPrecio(BigDecimal precio) {
        if (precio == null
                || precio.signum() < 0
                || precio.compareTo(PRECIO_MAXIMO) > 0
                || precio.stripTrailingZeros().scale() > 0) {

            throw new ReglaNegocioException(
                    "El precio debe estar entre 0 y 99999999 pesos enteros")
                    .conCodigo("PRECIO_TARIFA_INVALIDO");
        }
    }

    private TarifaResponse convertirRespuesta(TarifaVuelo tarifa) {
        return new TarifaResponse(
                tarifa.getId(),
                tarifa.getVuelo().getId(),
                tarifa.getClaseCabina(),
                tarifa.getPack().getId(),
                tarifa.getPack().getNombre(),
                tarifa.getPack().getActivo(),
                tarifa.getPrecio(),
                "CLP");
    }
}