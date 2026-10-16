package com.banduairlines.service.cuenta;

import com.banduairlines.model.Beneficio;
import com.banduairlines.model.enums.MotivoBloqueo;
import com.banduairlines.model.enums.Nivel;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Regla única de elegibilidad. La usan el catálogo (para motivoBloqueo)
 * y el canje (para lanzar el error correspondiente), así no se duplica.
 */
@Component
public class EvaluadorBeneficio {

    public Optional<MotivoBloqueo> evaluar(Beneficio beneficio, Nivel nivelUsuario, long saldoPuntos) {
        if (!beneficio.isDisponible()) {
            return Optional.of(MotivoBloqueo.BENEFICIO_AGOTADO);
        }
        if (nivelUsuario.compareTo(beneficio.getNivelRequerido()) < 0) {
            return Optional.of(MotivoBloqueo.NIVEL_INSUFICIENTE);
        }
        if (saldoPuntos < beneficio.getCostoPuntos()) {
            return Optional.of(MotivoBloqueo.PUNTOS_INSUFICIENTES);
        }
        return Optional.empty();
    }
}