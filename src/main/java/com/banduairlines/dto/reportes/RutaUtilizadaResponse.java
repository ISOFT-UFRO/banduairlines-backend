package com.banduairlines.dto.reportes;

public record RutaUtilizadaResponse(
        Long rutaId,
        String origenIata,
        String origenNombre,
        String destinoIata,
        String destinoNombre,
        Long boletosVendidos,
        Double porcentajeSobreTotal) {
}
