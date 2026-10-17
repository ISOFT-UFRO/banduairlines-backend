package com.banduairlines.service;

import com.banduairlines.dto.cuenta.MotivoBloqueo;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.service.cuenta.BeneficioConfigurado;
import com.banduairlines.service.cuenta.EvaluadorBeneficio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluadorBeneficioTest {

    private final EvaluadorBeneficio evaluador = new EvaluadorBeneficio();

    private static BeneficioConfigurado beneficio(Nivel nivel, int costo, boolean habilitado) {
        return new BeneficioConfigurado(1L, "Sala VIP", "Acceso a sala VIP", nivel, costo, 10, habilitado);
    }

    @Test
    void permiteCanjeCuandoCumpleTodo() {
        assertTrue(evaluador.evaluar(beneficio(Nivel.PLATA, 500, true), Nivel.ORO, 500, 3).isEmpty());
    }

    @Test
    void rechazaBeneficioNoDisponible() {
        assertEquals(MotivoBloqueo.BENEFICIO_AGOTADO,
                evaluador.evaluar(beneficio(Nivel.BASICO, 100, false), Nivel.ORO, 9999, 5).orElseThrow());
        assertEquals(MotivoBloqueo.BENEFICIO_AGOTADO,
                evaluador.evaluar(beneficio(Nivel.BASICO, 100, true), Nivel.ORO, 9999, 0).orElseThrow());
    }

    @Test
    void rechazaNivelInsuficiente() {
        assertEquals(MotivoBloqueo.NIVEL_INSUFICIENTE,
                evaluador.evaluar(beneficio(Nivel.ORO, 100, true), Nivel.PLATA, 9999, 5).orElseThrow());
    }

    @Test
    void rechazaPuntosInsuficientes() {
        assertEquals(MotivoBloqueo.PUNTOS_INSUFICIENTES,
                evaluador.evaluar(beneficio(Nivel.BASICO, 500, true), Nivel.BASICO, 499, 5).orElseThrow());
    }
}
