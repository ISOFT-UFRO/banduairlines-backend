package com.banduairlines.service.tracking.adaptador;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.banduairlines.service.tracking.RelojOperacional;
import com.banduairlines.service.tracking.puerto.VueloExterno;
import com.banduairlines.service.tracking.puerto.VueloExternoPort;

// Provisorio, solo para demostraciones: un vuelo por estado, con horarios relativos al primer uso.
@Component
@ConditionalOnProperty(name = "tracking.fuente-vuelos", havingValue = "demo")
public class VueloExternoDemoStub implements VueloExternoPort {

    static final int MINUTOS_REPROGRAMACION = 40;

    private final RelojOperacional reloj;
    private LocalDateTime base;
    private boolean primeraEntrega = true;

    public VueloExternoDemoStub(RelojOperacional reloj) {
        this.reloj = reloj;
    }

    @Override
    public synchronized List<VueloExterno> listarVuelosVigentes(LocalDateTime desde, LocalDateTime hasta) {
        // Base fija: si se recalculara en cada pasada, los vuelos se "reprogramarían" cada minuto.
        if (base == null) {
            base = reloj.ahora().truncatedTo(ChronoUnit.MINUTES);
        }
        // Ids y códigos únicos por arranque, para no mezclarse con seguimientos de una demostración anterior.
        long minutoBase = base.toEpochSecond(ZoneOffset.UTC) / 60;
        long idBase = 9_000_000_000L + minutoBase * 10;
        String codigoBase = "DM" + String.format("%03d", minutoBase % 1000);

        long reprogramacion = primeraEntrega ? 0 : MINUTOS_REPROGRAMACION;
        primeraEntrega = false;

        List<VueloExterno> vuelos = List.of(
                vuelo(idBase + 1, codigoBase + "1", "SCL", "PUQ", base.plusHours(3), base.plusHours(6)),
                vuelo(idBase + 2, codigoBase + "2", "SCL", "ANF", base.plusMinutes(30), base.plusMinutes(150)),
                vuelo(idBase + 3, codigoBase + "3", "CCP", "SCL", base.minusMinutes(10), base.plusMinutes(50)),
                vuelo(idBase + 4, codigoBase + "4", "PMC", "SCL", base.minusHours(2), base.plusMinutes(30)),
                vuelo(idBase + 5, codigoBase + "5", "CJC", "SCL", base.minusHours(5), base.minusHours(3)),
                vuelo(idBase + 6, codigoBase + "6", "SCL", "PMC",
                        base.plusHours(4).plusMinutes(reprogramacion), base.plusHours(6).plusMinutes(reprogramacion)));

        return vuelos.stream()
                .filter(v -> !v.salidaProgramada().isBefore(desde) && v.salidaProgramada().isBefore(hasta))
                .toList();
    }

    private static VueloExterno vuelo(long id, String codigo, String origen, String destino,
                                      LocalDateTime salida, LocalDateTime llegada) {
        return new VueloExterno(id, codigo, origen, destino, salida, llegada);
    }
}
