package com.banduairlines.dto.reportes;

public record ReservasPorEstadoResponse(
        String estado,
        long cantidad
) {
}