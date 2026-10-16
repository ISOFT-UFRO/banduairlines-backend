package com.banduairlines.service.reportes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reportes.AgrupacionTemporal;
import com.banduairlines.dto.reportes.EstadisticasComercialesResponse;
import com.banduairlines.dto.reportes.EstadoPagoMetricaResponse;
import com.banduairlines.dto.reportes.EstadoReservaMetricaResponse;
import com.banduairlines.dto.reportes.FiltroEstadisticasRequest;
import com.banduairlines.dto.reportes.OcupacionAsientosResponse;
import com.banduairlines.dto.reportes.RutaUtilizadaResponse;
import com.banduairlines.dto.reportes.SerieVentasResponse;
import com.banduairlines.dto.reportes.TarifaCabinaMetricaResponse;
import com.banduairlines.dto.reportes.TipoPasajeroMetricaResponse;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoExtraRepository;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PagoRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.VueloRepository;

@Service 
@Transactional(readOnly = true)
public class ReporteServiceImpl implements ReporteService{
    private final BoletoRepository boletoRepository;
    private final BoletoExtraRepository boletoExtraRepository;
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final VueloRepository vueloRepository;

    public ReporteServiceImpl(
            BoletoRepository boletoRepository,
            BoletoExtraRepository boletoExtraRepository,
            ReservaRepository reservaRepository,
            PagoRepository pagoRepository,
            VueloRepository vueloRepository) {
        this.boletoRepository = boletoRepository;
        this.boletoExtraRepository = boletoExtraRepository;
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.vueloRepository = vueloRepository;
    }

    public EstadisticasComercialesResponse obtener(FiltroEstadisticasRequest request) {
        FiltroEstadisticasRequest filtro = request == null
                ? vacio()
                : request.normalizado();

        validarRango(filtro.fechaInicio(), filtro.fechaFin());

        LocalDateTime inicio = filtro.fechaInicio() == null
                ? null
                : filtro.fechaInicio().atStartOfDay();

        LocalDateTime finExclusivo = filtro.fechaFin() == null
                ? null
                : filtro.fechaFin().plusDays(1).atStartOfDay();


        long totalVentas = valor(boletoRepository.contarVentasReportes(
                inicio, finExclusivo,
                filtro.rutaId(), filtro.origenIata(), filtro.destinoIata(),
                filtro.estadoReserva(), filtro.estadoPago(),
                filtro.tipoPasajero(), filtro.packTarifarioId(),
                filtro.claseCabina()));

        BigDecimal ingresosTotales = dinero(pagoRepository.sumarIngresosPagadosReportes(
                inicio, finExclusivo,
                filtro.rutaId(), filtro.origenIata(), filtro.destinoIata(),
                filtro.estadoReserva(), filtro.estadoPago(),
                filtro.tipoPasajero(), filtro.packTarifarioId(),
                filtro.claseCabina()));

        List<SerieVentasResponse> serie = construirSerie(
                filtro.agrupacion(),
                boletoRepository.ventasPorFechaReportes(
                        inicio, finExclusivo,
                        filtro.rutaId(), filtro.origenIata(), filtro.destinoIata(),
                        filtro.estadoReserva(), filtro.estadoPago(),
                        filtro.tipoPasajero(), filtro.packTarifarioId(),
                        filtro.claseCabina()),
                pagoRepository.ingresosPorFechaReportes(
                        inicio, finExclusivo,
                        filtro.rutaId(), filtro.origenIata(), filtro.destinoIata(),
                        filtro.estadoReserva(), filtro.estadoPago(),
                        filtro.tipoPasajero(), filtro.packTarifarioId(),
                        filtro.claseCabina()));

        long ocupados = valor(boletoRepository.contarAsientosOcupadosReportes(
                inicio, finExclusivo,
                filtro.rutaId(), filtro.origenIata(), filtro.destinoIata(),
                filtro.estadoReserva(), filtro.estadoPago(),
                filtro.tipoPasajero(), filtro.packTarifarioId(),
                filtro.claseCabina()));

        long capacidad = valor(vueloRepository.contarCapacidadAsientosReportes(
                inicio, finExclusivo,
                filtro.rutaId(), filtro.origenIata(), filtro.destinoIata(),
                filtro.estadoReserva(), filtro.estadoPago(),
                filtro.tipoPasajero(), filtro.packTarifarioId(),
                filtro.claseCabina()));

        List<RutaUtilizadaResponse> rutas = mapearRutas(
                boletoRepository.rankingRutasReportes(
                        inicio, finExclusivo,
                        filtro.rutaId(), filtro.origenIata(), filtro.destinoIata(),
                        filtro.estadoReserva(), filtro.estadoPago(),
                        filtro.tipoPasajero(), filtro.packTarifarioId(),
                        filtro.claseCabina()),
                totalVentas);

        List<EstadoReservaMetricaResponse> reservas = mapearEstadosReserva(
                reservaRepository.estadosReservaReportes(
                        inicio, finExclusivo,
                        filtro.rutaId(), filtro.origenIata(), filtro.destinoIata(),
                        filtro.estadoReserva(), filtro.estadoPago(),
                        filtro.tipoPasajero(), filtro.packTarifarioId(),
                        filtro.claseCabina()));

        List<EstadoPagoMetricaResponse> pagos = mapearEstadosPago(
                pagoRepository.estadosPagoReportes(
                        inicio, finExclusivo,
                        filtro.rutaId(), filtro.origenIata(), filtro.destinoIata(),
                        filtro.estadoReserva(), filtro.estadoPago(),
                        filtro.tipoPasajero(), filtro.packTarifarioId(),
                        filtro.claseCabina()));

        List<TipoPasajeroMetricaResponse> pasajeros = mapearPasajeros(
                boletoRepository.pasajerosPorTipoReportes(
                        inicio, finExclusivo,
                        filtro.rutaId(), filtro.origenIata(), filtro.destinoIata(),
                        filtro.estadoReserva(), filtro.estadoPago(),
                        filtro.tipoPasajero(), filtro.packTarifarioId(),
                        filtro.claseCabina()));

        List<TarifaCabinaMetricaResponse> tarifas = mapearTarifas(
                boletoRepository.tarifasPorPackYClaseReportes(
                        inicio, finExclusivo,
                        filtro.rutaId(), filtro.origenIata(), filtro.destinoIata(),
                        filtro.estadoReserva(), filtro.estadoPago(),
                        filtro.tipoPasajero(), filtro.packTarifarioId(),
                        filtro.claseCabina()),
                boletoExtraRepository.ingresosExtrasPorTarifaReportes(
                        inicio, finExclusivo,
                        filtro.rutaId(), filtro.origenIata(), filtro.destinoIata(),
                        filtro.estadoReserva(), filtro.estadoPago(),
                        filtro.tipoPasajero(), filtro.packTarifarioId(),
                        filtro.claseCabina()));

        return new EstadisticasComercialesResponse(
                totalVentas,
                ingresosTotales,
                serie,
                new OcupacionAsientosResponse(
                        ocupados,
                        capacidad,
                        porcentaje(ocupados, capacidad)),
                rutas,
                reservas,
                pagos,
                pasajeros,
                tarifas);
    }

    private static List<RutaUtilizadaResponse> mapearRutas(
            List<Object[]> rows,
            long totalVentas) {

        return rows.stream()
                .map(row -> new RutaUtilizadaResponse(
                        (Long) row[0],
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        ((Number) row[5]).longValue(),
                        porcentaje(((Number) row[5]).longValue(), totalVentas)))
                .toList();
    }

    private static List<EstadoReservaMetricaResponse> mapearEstadosReserva(
            List<Object[]> rows) {

        Map<EstadoReserva, EstadoReservaMetricaResponse> encontrados =
                new EnumMap<>(EstadoReserva.class);

        rows.forEach(row -> encontrados.put(
                (EstadoReserva) row[0],
                new EstadoReservaMetricaResponse(
                        (EstadoReserva) row[0],
                        ((Number) row[1]).longValue(),
                        dinero((BigDecimal) row[2]))));

        List<EstadoReservaMetricaResponse> result = new ArrayList<>();

        for (EstadoReserva estado : EstadoReserva.values()) {
            result.add(encontrados.getOrDefault(
                    estado,
                    new EstadoReservaMetricaResponse(
                            estado, 0L, BigDecimal.ZERO)));
        }

        return result;
    }

    private static List<EstadoPagoMetricaResponse> mapearEstadosPago(
            List<Object[]> rows) {

        Map<EstadoPago, EstadoPagoMetricaResponse> encontrados =
                new EnumMap<>(EstadoPago.class);

        rows.forEach(row -> encontrados.put(
                (EstadoPago) row[0],
                new EstadoPagoMetricaResponse(
                        (EstadoPago) row[0],
                        ((Number) row[1]).longValue(),
                        dinero((BigDecimal) row[2]))));

        List<EstadoPagoMetricaResponse> result = new ArrayList<>();

        for (EstadoPago estado : EstadoPago.values()) {
            result.add(encontrados.getOrDefault(
                    estado,
                    new EstadoPagoMetricaResponse(
                            estado, 0L, BigDecimal.ZERO)));
        }

        return result;
    }

    private static List<TipoPasajeroMetricaResponse> mapearPasajeros(
            List<Object[]> rows) {

        Map<TipoPasajero, Long> cantidades = new EnumMap<>(TipoPasajero.class);

        rows.forEach(row -> cantidades.put(
                (TipoPasajero) row[0],
                ((Number) row[1]).longValue()));

        long total = cantidades.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        List<TipoPasajeroMetricaResponse> result = new ArrayList<>();

        for (TipoPasajero tipo : TipoPasajero.values()) {
            long cantidad = cantidades.getOrDefault(tipo, 0L);
            result.add(new TipoPasajeroMetricaResponse(
                    tipo,
                    cantidad,
                    porcentaje(cantidad, total)));
        }

        return result;
    }

    private static List<TarifaCabinaMetricaResponse> mapearTarifas(
            List<Object[]> tarifas,
            List<Object[]> extras) {

        Map<ClaveTarifa, BigDecimal> extrasPorTarifa = new LinkedHashMap<>();

        extras.forEach(row -> extrasPorTarifa.put(
                new ClaveTarifa(
                        (Long) row[0],
                        (ClaseCabina) row[1]),
                dinero((BigDecimal) row[2])));

        return tarifas.stream()
                .map(row -> {
                    Long packId = (Long) row[0];
                    String packNombre = (String) row[1];
                    ClaseCabina clase = (ClaseCabina) row[2];
                    long cantidad = ((Number) row[3]).longValue();
                    BigDecimal tarifaBase = dinero((BigDecimal) row[4]);

                    BigDecimal extrasMonto = extrasPorTarifa.getOrDefault(
                            new ClaveTarifa(packId, clase),
                            BigDecimal.ZERO);

                    return new TarifaCabinaMetricaResponse(
                            packId,
                            packNombre,
                            clase,
                            cantidad,
                            tarifaBase.add(extrasMonto));
                })
                .toList();
    }

    private static List<SerieVentasResponse> construirSerie(
            AgrupacionTemporal agrupacion,
            List<Object[]> ventas,
            List<Object[]> ingresos) {

        AgrupacionTemporal efectiva = agrupacion == null
                ? AgrupacionTemporal.DIARIA
                : agrupacion;

        Map<String, Acumulado> mapa = new LinkedHashMap<>();

        ventas.forEach(row -> {
            LocalDateTime fecha = (LocalDateTime) row[0];
            long cantidad = ((Number) row[1]).longValue();
            String clave = clavePeriodo(fecha, efectiva);

            mapa.computeIfAbsent(clave, ignored -> new Acumulado())
                    .ventas += cantidad;
        });

        ingresos.forEach(row -> {
            LocalDateTime fecha = (LocalDateTime) row[0];
            BigDecimal monto = dinero((BigDecimal) row[1]);
            String clave = clavePeriodo(fecha, efectiva);

            Acumulado acumulado =
                    mapa.computeIfAbsent(clave, ignored -> new Acumulado());

            acumulado.ingresos = acumulado.ingresos.add(monto);
        });

        return mapa.entrySet().stream()
                .map(entry -> new SerieVentasResponse(
                        entry.getKey(),
                        entry.getValue().ventas,
                        entry.getValue().ingresos))
                .toList();
    }

    private static String clavePeriodo(
            LocalDateTime fecha,
            AgrupacionTemporal agrupacion) {

        LocalDate dia = fecha.toLocalDate();

        return switch (agrupacion) {
            case DIARIA -> dia.toString();
            case SEMANAL -> dia
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .toString();
            case MENSUAL -> "%04d-%02d".formatted(
                    dia.getYear(), dia.getMonthValue());
            case ANUAL -> String.valueOf(dia.getYear());
        };
    }

    private static void validarRango(LocalDate inicio, LocalDate fin) {
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            throw new ReglaNegocioException(
                    "La fecha de inicio no puede ser posterior a la fecha de fin")
                    .conCodigo("RANGO_FECHAS_INVALIDO");
        }
    }

    private static FiltroEstadisticasRequest vacio() {
        return new FiltroEstadisticasRequest(
                null, null, null, null, null, null,
                null, null, null, null, null);
    }

    private static long valor(Long value) {
        return value == null ? 0L : value;
    }

    private static BigDecimal dinero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static double porcentaje(long parte, long total) {
        if (total <= 0L) {
            return 0.0;
        }

        return BigDecimal.valueOf(parte)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(total),
                        2,
                        RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record ClaveTarifa(Long packId, ClaseCabina clase) {
    }
    private static final class Acumulado {
        private long ventas;
        private BigDecimal ingresos = BigDecimal.ZERO;
    }
}
