package com.banduairlines.service;

import com.banduairlines.model.Beneficio;
import com.banduairlines.model.enums.MotivoBloqueo;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.service.fidelizacion.EvaluadorBeneficio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluadorBeneficioTest {

    private final EvaluadorBeneficio evaluador = new EvaluadorBeneficio();

    private Beneficio beneficio(Nivel nivel, int costo, boolean disponible) {
        return Beneficio.builder()
                .nombre("Sala VIP")
                .nivelRequerido(nivel)
                .costoPuntos(costo)
                .disponible(disponible)
                .build();
    }

    @Test
    void permiteCanjeCuandoCumpleTodo() {
        assertTrue(evaluador.evaluar(beneficio(Nivel.PLATA, 500, true), Nivel.ORO, 500).isEmpty());
    }

    @Test
    void rechazaBeneficioNoDisponible() {
        assertEquals(MotivoBloqueo.BENEFICIO_AGOTADO,
                evaluador.evaluar(beneficio(Nivel.BRONCE, 100, false), Nivel.ORO, 9999).orElseThrow());
    }

    @Test
    void rechazaNivelInsuficiente() {
        assertEquals(MotivoBloqueo.NIVEL_INSUFICIENTE,
                evaluador.evaluar(beneficio(Nivel.ORO, 100, true), Nivel.PLATA, 9999).orElseThrow());
    }

    @Test
    void rechazaPuntosInsuficientes() {
        assertEquals(MotivoBloqueo.PUNTOS_INSUFICIENTES,
                evaluador.evaluar(beneficio(Nivel.BRONCE, 500, true), Nivel.BRONCE, 499).orElseThrow());
    }
}