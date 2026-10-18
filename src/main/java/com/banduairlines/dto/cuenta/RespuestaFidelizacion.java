package com.banduairlines.dto.cuenta;

import com.banduairlines.model.enums.Nivel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record RespuestaFidelizacion(
        @Schema(description = "Saldo calculado desde el historial: acumulaciones menos canjes")
        long puntosDisponibles,
        @Schema(description = "Millas acumuladas por los vuelos realizados")
        BigDecimal millasAcumuladas,
        Nivel nivelActual,
        BigDecimal multiplicadorPuntos,
        ProgresoNivel progreso,
        @Schema(description = "Beneficios que el pasajero puede canjear ahora")
        List<RespuestaBeneficio> beneficiosDisponibles,
        @Schema(description = "Últimos 5 movimientos, del más reciente al más antiguo")
        List<RespuestaMovimientoPuntos> movimientosRecientes
) {
}
