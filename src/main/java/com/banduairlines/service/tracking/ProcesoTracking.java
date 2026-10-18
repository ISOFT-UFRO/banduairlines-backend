package com.banduairlines.service.tracking;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.service.tracking.puerto.VueloExterno;
import com.banduairlines.service.tracking.puerto.VueloExternoPort;

// RF-05. Sin endpoint: su actor es el paso del tiempo.
@Component
@ConditionalOnProperty(name = "tracking.proceso.habilitado", havingValue = "true", matchIfMissing = true)
public class ProcesoTracking {

    private static final Logger log = LoggerFactory.getLogger(ProcesoTracking.class);

    private static final Set<EstadoVuelo> TERMINALES = Arrays.stream(EstadoVuelo.values())
            .filter(MaquinaEstadosVuelo::esTerminal)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(EstadoVuelo.class)));

    private final VueloExternoPort vuelos;
    private final SeguimientoVueloRepository seguimientos;
    private final SincronizadorSeguimiento sincronizador;
    private final RelojOperacional reloj;
    private final TrackingProperties.Sincronizacion ventana;

    public ProcesoTracking(VueloExternoPort vuelos, SeguimientoVueloRepository seguimientos,
                           SincronizadorSeguimiento sincronizador, RelojOperacional reloj,
                           TrackingProperties properties) {
        this.vuelos = vuelos;
        this.seguimientos = seguimientos;
        this.sincronizador = sincronizador;
        this.reloj = reloj;
        this.ventana = properties.sincronizacion();
    }

    @Scheduled(fixedDelayString = "${tracking.proceso.intervalo-ms:60000}")
    public void ejecutarPasada() {
        LocalDateTime ahora = reloj.ahora();

        for (VueloExterno vuelo : vuelos.listarVuelosVigentes(
                ahora.minusDays(ventana.diasAtras()), ahora.plusDays(ventana.diasAdelante()))) {
            try {
                sincronizador.sincronizar(vuelo, ahora);
            } catch (RuntimeException e) {
                log.warn("No se pudo sincronizar el vuelo {}: {}", vuelo.vueloId(), e.getMessage(), e);
            }
        }

        for (SeguimientoVuelo seguimiento : seguimientos.findByEstadoActualNotIn(TERMINALES)) {
            try {
                sincronizador.actualizar(seguimiento.getId(), ahora);
            } catch (RuntimeException e) {
                log.warn("No se pudo actualizar el seguimiento {}: {}", seguimiento.getId(), e.getMessage(), e);
            }
        }
    }
}
