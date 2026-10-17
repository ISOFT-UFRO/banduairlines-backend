package com.banduairlines.dto.reserva;

import java.util.List;

//endpoint 15 dto
public record PaginaReservasResponse(
        List<ReservaResumen> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {
}

