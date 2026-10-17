package com.banduairlines.service.cuenta;

import com.banduairlines.model.enums.Nivel;

/**
 * Beneficio del programa de fidelización definido en la configuración (app.fidelizacion.beneficios).
 * No es una entidad: el catálogo no se guarda en la base de datos.
 *
 * @param id             identificador estable, asignado a mano y nunca reutilizado
 * @param stockInicial   unidades totales; las disponibles se calculan restando los canjes registrados
 * @param habilitado     si es false, el beneficio no se puede canjear (null se interpreta como true)
 */
public record BeneficioConfigurado(
        Long id,
        String nombre,
        String descripcion,
        Nivel nivelRequerido,
        Integer costoPuntos,
        Integer stockInicial,
        Boolean habilitado
) {

    public boolean estaHabilitado() {
        return !Boolean.FALSE.equals(habilitado);
    }
}
