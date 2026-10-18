package com.banduairlines.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.model.enums.OrigenEvento;
import com.banduairlines.repository.SeguimientoVueloRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

// Requiere el Postgres local. Cada test se revierte al terminar.
@Tag("bd")
@Transactional
@SpringBootTest(properties = "tracking.proceso.habilitado=false")
class SeguimientoVueloPersistenciaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 16, 9, 0);

    @Autowired
    private SeguimientoVueloRepository seguimientos;

    @Autowired
    private EntityManager em;

    private SeguimientoVuelo guardarSeguimiento(Long vueloId) {
        SeguimientoVuelo s = SeguimientoVuelo.iniciar(vueloId, "TST-" + Math.abs(vueloId), "SCL", "PMC",
                AHORA.plusDays(1), AHORA.plusDays(1).plusHours(2), AHORA);
        seguimientos.saveAndFlush(s);
        return s;
    }

    @Test
    void eventoPersistido_alModificarloYHacerFlush_laFilaNoCambia() {
        Long eventoId = guardarSeguimiento(-1L).getEventos().get(0).getId();

        EventoEstado evento = em.find(EventoEstado.class, eventoId);
        ReflectionTestUtils.setField(evento, "motivo", "alterado");
        ReflectionTestUtils.setField(evento, "origen", OrigenEvento.MANUAL);
        em.flush();
        em.clear();

        EventoEstado releido = em.find(EventoEstado.class, eventoId);
        assertThat(releido.getMotivo()).isNull();
        assertThat(releido.getOrigen()).isEqualTo(OrigenEvento.SISTEMA);
    }

    @Test
    void borrarSeguimientoConEventos_fallaPorLaFk() {
        Long id = guardarSeguimiento(-2L).getId();
        em.clear();

        assertThatThrownBy(() -> em.createNativeQuery("delete from seguimiento_vuelo where id = :id")
                .setParameter("id", id)
                .executeUpdate())
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("evento_estado_seguimiento_vuelo_id_foreign");
    }
}
