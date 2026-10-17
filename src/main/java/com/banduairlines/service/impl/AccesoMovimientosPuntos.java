package com.banduairlines.service.impl;

import com.banduairlines.exception.ServicioExternoException;
import com.banduairlines.service.cuenta.CodigosFidelizacion;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Entrega la implementación de {@link FuenteMovimientosPuntos}. Si no hay ninguna registrada, las operaciones que
 * dependen de los movimientos responden 502 MOVIMIENTOS_NO_DISPONIBLES en lugar de inventar datos.
 */
@Component
public class AccesoMovimientosPuntos {

    private final ObjectProvider<FuenteMovimientosPuntos> fuente;

    public AccesoMovimientosPuntos(ObjectProvider<FuenteMovimientosPuntos> fuente) {
        this.fuente = fuente;
    }

    public FuenteMovimientosPuntos exigir() {
        FuenteMovimientosPuntos disponible = fuente.getIfUnique();
        if (disponible == null) {
            throw new ServicioExternoException("El registro de movimientos de puntos no está disponible")
                    .conCodigo(CodigosFidelizacion.MOVIMIENTOS_NO_DISPONIBLES);
        }
        return disponible;
    }
}
