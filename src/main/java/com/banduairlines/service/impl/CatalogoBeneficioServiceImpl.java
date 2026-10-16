package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.ActualizacionFidelizacion;
import com.banduairlines.dto.cuenta.RespuestaBeneficio;
import com.banduairlines.repository.BeneficioRepository;
import com.banduairlines.service.cuenta.ActualizacionFidelizacionService;
import com.banduairlines.service.cuenta.CatalogoBeneficioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogoBeneficioServiceImpl implements CatalogoBeneficioService {

    private final ActualizacionFidelizacionService actualizacion;
    private final BeneficioRepository beneficios;
    private final MapeadorFidelizacion mapeador;

    public CatalogoBeneficioServiceImpl(ActualizacionFidelizacionService actualizacion,
                                        BeneficioRepository beneficios, MapeadorFidelizacion mapeador) {
        this.actualizacion = actualizacion;
        this.beneficios = beneficios;
        this.mapeador = mapeador;
    }

    /** Devuelve todo el catálogo, indicando para el pasajero si cada beneficio se puede canjear y, si no, por qué. */
    @Override
    @Transactional
    public List<RespuestaBeneficio> listarActual() {
        ActualizacionFidelizacion estado = actualizacion.actualizarActual();
        return beneficios.findAllByOrderByCostoPuntosAscNombreAsc().stream()
                .map(beneficio -> mapeador.beneficio(beneficio, estado.nivelActual(), estado.saldoPuntos()))
                .toList();
    }
}
