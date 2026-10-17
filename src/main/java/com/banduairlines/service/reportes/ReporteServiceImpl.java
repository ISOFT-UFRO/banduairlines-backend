package com.banduairlines.service.reportes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reportes.ResumenMensualResponse;
import com.banduairlines.repository.PagoRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.VueloRepository;

@Service
@Transactional(readOnly = true)
public class ReporteServiceImpl implements ReporteService {
    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-CL");

    private final ReservaRepository reservaRepository;
    private final VueloRepository vueloRepository;
    private final PagoRepository pagoRepository;
    private final Clock clock;

    public ReporteServiceImpl(
            ReservaRepository reservaRepository,
            VueloRepository vueloRepository,
            PagoRepository pagoRepository,
            Clock clock) {
        this.reservaRepository = reservaRepository;
        this.vueloRepository = vueloRepository;
        this.pagoRepository = pagoRepository;
        this.clock = clock;
    }

    @Override
    public List<ResumenMensualResponse> listar() {
        LocalDate hoy = LocalDate.now(clock);
        LocalDateTime inicioMesActual = hoy.withDayOfMonth(1).atStartOfDay();

        Set<YearMonth> periodos = new LinkedHashSet<>();

        reservaRepository.buscarMesesHistoricosReportes(inicioMesActual)
                .stream().map(this::toYearMonth).forEach(periodos::add);

        vueloRepository.buscarMesesHistoricosReportes(inicioMesActual)
                .stream().map(this::toYearMonth).forEach(periodos::add);

        pagoRepository.buscarMesesHistoricosReportes(inicioMesActual)
                .stream().map(this::toYearMonth).forEach(periodos::add);

        return periodos.stream()
                .sorted(Comparator.reverseOrder())
                .map(this::toResponse)
                .toList();
    }

    private YearMonth toYearMonth(Object[] row) {
        return YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
    }

    private ResumenMensualResponse toResponse(YearMonth periodo) {
        String nombreMes = Month.of(periodo.getMonthValue())
                .getDisplayName(TextStyle.FULL, LOCALE_ES);
        String nombrePeriodo = Character.toUpperCase(nombreMes.charAt(0))
                + nombreMes.substring(1) + " " + periodo.getYear();
        return new ResumenMensualResponse(
                periodo.toString(), periodo.getYear(), periodo.getMonthValue(), nombrePeriodo);
    }
    
}
