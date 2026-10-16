package com.banduairlines.dto.reportes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EstadisticasResponse(
        LocalDate desde,
        LocalDate hasta,
        String moneda,
        BigDecimal totalIngresos,
        long totalReservas,
        long reservasConfirmadas,
        long asientosAsignados,
        List<IngresoDiario> ingresosPorDia,
        List<ReservasPorEstado> reservasPorEstado,
        List<PasajerosPorTipo> pasajerosPorTipo
) {

    public record IngresoDiario(
            LocalDate fecha,
            BigDecimal ingresos
    ) {
    }

    public record ReservasPorEstado(
            String estado,
            long cantidad
    ) {
    }

    public record PasajerosPorTipo(
            String tipo,
            long cantidad
    ) {
    }
}