package com.banduairlines.dto.reportes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(
        LocalDate desde,
        LocalDate hasta,
        String agrupacion,
        long pagosConfirmados,
        List<IngresoPorMoneda> ingresosTotales,
        List<IngresoPeriodo> ingresosPorPeriodo,
        List<IngresoPorRuta> ingresosPorRuta,
        List<VentaExtra> ventasDeExtras,
        OcupacionTotal ocupacionTotal,
        List<OcupacionVuelo> ocupacionPorVuelo
) {
    public record IngresoPorMoneda(String moneda, BigDecimal ingresos) { }

    public record IngresoPeriodo(LocalDate periodo, String moneda, BigDecimal ingresos, long pagos) { }

    public record IngresoPorRuta(String origen, String destino, String moneda,
                                 BigDecimal ingresos, long boletos) { }

    public record VentaExtra(String nombre, String tipo, String moneda,
                             BigDecimal ingresos, long unidades) { }

    public record OcupacionTotal(long asientosOcupados, long capacidad, BigDecimal porcentaje) { }

    public record OcupacionVuelo(String codigoVuelo, String origen, String destino,
                                 LocalDateTime salidaProgramada, long asientosOcupados,
                                 long capacidad, BigDecimal porcentaje) { }
}

