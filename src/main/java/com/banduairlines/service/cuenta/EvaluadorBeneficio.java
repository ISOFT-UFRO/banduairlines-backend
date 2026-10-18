package com.banduairlines.service.cuenta;

import com.banduairlines.dto.cuenta.MotivoBloqueo;
import com.banduairlines.model.enums.Nivel;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Regla única de elegibilidad. La usan el catálogo (para motivoBloqueo)
 * y el canje (para lanzar el error correspondiente), así no se duplica.
 * Orden: deshabilitado o agotado, luego nivel, luego puntos.
 */
@Component
public class EvaluadorBeneficio {

    public Optional<MotivoBloqueo> evaluar(BeneficioConfigurado beneficio, Nivel nivelUsuario,
                                           long saldoPuntos, long stockRestante) {
        if (!beneficio.estaHabilitado() || stockRestante <= 0) {
            return Optional.of(MotivoBloqueo.BENEFICIO_AGOTADO);
        }
        if (nivelUsuario.compareTo(beneficio.nivelRequerido()) < 0) {
            return Optional.of(MotivoBloqueo.NIVEL_INSUFICIENTE);
        }
        if (saldoPuntos < beneficio.costoPuntos()) {
            return Optional.of(MotivoBloqueo.PUNTOS_INSUFICIENTES);
        }
        return Optional.empty();
    }
}
