package com.banduairlines.service.reportes;

import com.banduairlines.dto.reportes.DashboardResponse;
import com.banduairlines.dto.reportes.IngresoDiarioProjection;
import com.banduairlines.dto.reportes.IngresoRutaProjection;
import com.banduairlines.dto.reportes.IngresoPorMonedaProjection;
import com.banduairlines.dto.reportes.OcupacionVueloProjection;
import com.banduairlines.dto.reportes.VentaExtraProjection;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PagoRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.VueloRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final VueloRepository vueloRepository;
    private final BoletoRepository boletoRepository;

    public DashboardServiceImpl(PagoRepository pagoRepository, ReservaRepository reservaRepository,
            VueloRepository vueloRepository, BoletoRepository boletoRepository) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.vueloRepository = vueloRepository;
        this.boletoRepository = boletoRepository;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DashboardResponse obtener(LocalDate desde, LocalDate hasta, String agrupacion) {
        if (desde == null || hasta == null || hasta.isBefore(desde)
                || hasta.isEqual(LocalDate.MAX) || ChronoUnit.DAYS.between(desde, hasta) >= 366) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El período debe estar ordenado y contener como máximo 366 días");
        }
        String tipoAgrupacion = normalizarAgrupacion(agrupacion);
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();

        List<DashboardResponse.IngresoPorMoneda> ingresosTotales = pagoRepository
                .sumarIngresosPorMoneda(inicio, finExclusivo).stream()
                .map(this::mapearIngresoMoneda).toList();

        List<DashboardResponse.IngresoPeriodo> ingresosPorPeriodo = agruparIngresos(
                desde, hasta, tipoAgrupacion,
                /*pagoRepository.obtenerIngresosDiarios(inicio, finExclusivo)*/
                crearIngresosDiariosMock(),
                 ingresosTotales);

        List<DashboardResponse.IngresoPorRuta> ingresosPorRuta = reservaRepository
                .resumirIngresosPorRuta(inicio, finExclusivo).stream()
                .map(this::mapearIngresoRuta).toList();

        List<DashboardResponse.VentaExtra> ventasExtras = boletoRepository
                .resumirVentasExtras(inicio, finExclusivo).stream()
                .map(this::mapearVentaExtra).toList();
        //limitado hasta implementacion de VueloRepository
        /*List<DashboardResponse.OcupacionVuelo> vuelos = vueloRepository
                .resumirOcupacion(inicio, finExclusivo).stream()
                .map(this::mapearOcupacion).toList();*/

        List<DashboardResponse.OcupacionVuelo> vuelos = crearOcupacionMock();

        long asientosOcupados = vuelos.stream()
                .mapToLong(DashboardResponse.OcupacionVuelo::asientosOcupados).sum();
        long capacidad = vuelos.stream().mapToLong(DashboardResponse.OcupacionVuelo::capacidad).sum();

        return new DashboardResponse(
                desde, hasta, tipoAgrupacion,
                pagoRepository.contarPagosConfirmados(inicio, finExclusivo),
                ingresosTotales, ingresosPorPeriodo, ingresosPorRuta, ventasExtras,
                new DashboardResponse.OcupacionTotal(asientosOcupados, capacidad,
                        porcentaje(asientosOcupados, capacidad)),
                vuelos);
    }

    private List<DashboardResponse.IngresoPeriodo> agruparIngresos(LocalDate desde, LocalDate hasta,
            String agrupacion, List<IngresoDiarioProjection> filas,
            List<DashboardResponse.IngresoPorMoneda> monedas) {
        Map<LocalDate, Map<String, Acumulado>> agrupados = new TreeMap<>();
        for (IngresoDiarioProjection fila : filas) {
            LocalDate periodo = inicioPeriodo(fila.getFecha(), agrupacion);
            Acumulado acumulado = agrupados.computeIfAbsent(periodo, ignorado -> new LinkedHashMap<>())
                    .computeIfAbsent(fila.getMoneda(), ignorado -> new Acumulado());
            acumulado.ingresos = acumulado.ingresos.add(fila.getIngresos());
            acumulado.pagos += fila.getPagos();
        }

        List<String> monedasPresentes = monedas.isEmpty()
                ? List.of("CLP") : monedas.stream().map(DashboardResponse.IngresoPorMoneda::moneda).toList();
        LocalDate periodo = inicioPeriodo(desde, agrupacion);
        LocalDate ultimoPeriodo = inicioPeriodo(hasta, agrupacion);
        List<DashboardResponse.IngresoPeriodo> serie = new ArrayList<>();
        while (!periodo.isAfter(ultimoPeriodo)) {
            Map<String, Acumulado> datosPeriodo = agrupados.getOrDefault(periodo, Map.of());
            for (String moneda : monedasPresentes) {
                Acumulado acumulado = datosPeriodo.getOrDefault(moneda, new Acumulado());
                serie.add(new DashboardResponse.IngresoPeriodo(periodo, moneda,
                        acumulado.ingresos, acumulado.pagos));
            }
            periodo = avanzar(periodo, agrupacion);
        }
        return List.copyOf(serie);
    }

    private LocalDate inicioPeriodo(LocalDate fecha, String agrupacion) {
        return switch (agrupacion) {
            case "dia" -> fecha;
            case "semana" -> fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "mes" -> fecha.withDayOfMonth(1);
            default -> fecha.withDayOfYear(1);
        };
    }

    private LocalDate avanzar(LocalDate fecha, String agrupacion) {
        return switch (agrupacion) {
            case "dia" -> fecha.plusDays(1);
            case "semana" -> fecha.plusWeeks(1);
            case "mes" -> fecha.plusMonths(1);
            default -> fecha.plusYears(1);
        };
    }

    private String normalizarAgrupacion(String agrupacion) {
        if (agrupacion == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La agrupación debe ser dia, semana, mes o anio");
        }
        return switch (agrupacion.toLowerCase(Locale.ROOT)) {
            case "dia", "día" -> "dia";
            case "semana" -> "semana";
            case "mes" -> "mes";
            case "anio", "año" -> "anio";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La agrupación debe ser dia, semana, mes o anio");
        };
    }

    private DashboardResponse.IngresoPorMoneda mapearIngresoMoneda(IngresoPorMonedaProjection fila) {
        return new DashboardResponse.IngresoPorMoneda(fila.getMoneda(), fila.getIngresos());
    }

    private DashboardResponse.IngresoPorRuta mapearIngresoRuta(IngresoRutaProjection fila) {
        return new DashboardResponse.IngresoPorRuta(fila.getOrigen(), fila.getDestino(),
                fila.getMoneda(), fila.getIngresos(), fila.getBoletos());
    }

    private DashboardResponse.VentaExtra mapearVentaExtra(VentaExtraProjection fila) {
        return new DashboardResponse.VentaExtra(fila.getNombre(), fila.getTipo(), fila.getMoneda(),
                fila.getIngresos(), fila.getUnidades());
    }

    private DashboardResponse.OcupacionVuelo mapearOcupacion(OcupacionVueloProjection fila) {
        return new DashboardResponse.OcupacionVuelo(fila.getCodigoVuelo(), fila.getOrigen(),
                fila.getDestino(), fila.getSalidaProgramada(), fila.getAsientosOcupados(),
                fila.getCapacidad(), porcentaje(fila.getAsientosOcupados(), fila.getCapacidad()));
    }

    private BigDecimal porcentaje(long ocupados, long capacidad) {
        return capacidad == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(ocupados).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(capacidad), 2, RoundingMode.HALF_UP);
    }

    private static final class Acumulado {
        private BigDecimal ingresos = BigDecimal.ZERO;
        private long pagos;
    }

    // MOCK TEMPORAL
    private List<DashboardResponse.OcupacionVuelo> crearOcupacionMock() {
    return List.of(
            new DashboardResponse.OcupacionVuelo(
                    "BA101",
                    "SCL",
                    "ZCO",
                    LocalDateTime.of(2026, 10, 15, 8, 30),
                    120L,
                    180L,
                    porcentaje(120L, 180L)
            ),
            new DashboardResponse.OcupacionVuelo(
                    "BA202",
                    "SCL",
                    "CCP",
                    LocalDateTime.of(2026, 10, 15, 12, 15),
                    150L,
                    180L,
                    porcentaje(150L, 180L)
            ),
            new DashboardResponse.OcupacionVuelo(
                    "BA303",
                    "ZCO",
                    "SCL",
                    LocalDateTime.of(2026, 10, 15, 18, 45),
                    90L,
                    150L,
                    porcentaje(90L, 150L)
            )
    );
}   

private List<IngresoDiarioProjection> crearIngresosDiariosMock() {

    IngresoDiarioProjection fila1 = new IngresoDiarioProjection() {

        @Override
        public LocalDate getFecha() {
            return LocalDate.of(2026, 10, 1);
        }

        @Override
        public String getMoneda() {
            return "CLP";
        }

        @Override
        public BigDecimal getIngresos() {
            return new BigDecimal("500000");
        }

        @Override
        public long getPagos() {
            return 5L;
        }
    };

    return List.of(fila1);
}
}
