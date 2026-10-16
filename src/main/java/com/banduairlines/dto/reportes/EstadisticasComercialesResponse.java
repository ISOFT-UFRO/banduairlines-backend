package com.banduairlines.dto.reportes;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Consolidado de estadísticas comerciales")
public record EstadisticasComercialesResponse(
        Long totalVentas,
        BigDecimal ingresosTotales,
        List<SerieVentasResponse> serieTemporal,
        OcupacionAsientosResponse ocupacionAsientos,
        List<RutaUtilizadaResponse> rutasMasUtilizadas,
        List<EstadoReservaMetricaResponse> estadosReserva,
        List<EstadoPagoMetricaResponse> estadosPago,
        List<TipoPasajeroMetricaResponse> tiposPasajero,
        List<TarifaCabinaMetricaResponse> tarifasYClases) {
}
