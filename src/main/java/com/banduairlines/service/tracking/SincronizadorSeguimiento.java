package com.banduairlines.service.tracking;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.service.tracking.puerto.VueloExterno;

// Una transacción por vuelo: el fallo de uno no revierte a los demás. Bean separado de ProcesoTracking
// porque @Transactional no aplica en llamadas dentro de la misma clase.
@Component
public class SincronizadorSeguimiento {

    private final SeguimientoVueloRepository seguimientos;
    private final EvolucionHoraria evolucion;

    public SincronizadorSeguimiento(SeguimientoVueloRepository seguimientos, EvolucionHoraria evolucion) {
        this.seguimientos = seguimientos;
        this.evolucion = evolucion;
    }

    @Transactional
    public void sincronizar(VueloExterno vuelo, LocalDateTime ahora) {
        seguimientos.findByVueloId(vuelo.vueloId()).ifPresentOrElse(
                seguimiento -> seguimiento.sincronizar(vuelo.codigoVuelo(), vuelo.origenIata(), vuelo.destinoIata(),
                        vuelo.salidaProgramada(), vuelo.llegadaProgramada()),
                () -> seguimientos.save(SeguimientoVuelo.iniciar(vuelo.vueloId(), vuelo.codigoVuelo(),
                        vuelo.origenIata(), vuelo.destinoIata(), vuelo.salidaProgramada(),
                        vuelo.llegadaProgramada(), ahora)));
    }

    @Transactional
    public void actualizar(Long seguimientoId, LocalDateTime ahora) {
        seguimientos.buscarConHistorialPorId(seguimientoId)
                .ifPresent(seguimiento -> evolucion.aplicar(seguimiento, ahora));
    }
}
