package com.banduairlines.service.cuenta;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Catálogo de beneficios cargado desde app.fidelizacion.beneficios[n].*.
 * Si no hay beneficios configurados, el catálogo queda vacío.
 */
@ConfigurationProperties(prefix = "app.fidelizacion")
public record PropiedadesFidelizacion(List<BeneficioConfigurado> beneficios) {

    public PropiedadesFidelizacion {
        beneficios = beneficios == null ? List.of() : List.copyOf(beneficios);
    }
}
