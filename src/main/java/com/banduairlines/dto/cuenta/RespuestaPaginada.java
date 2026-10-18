package com.banduairlines.dto.cuenta;

import java.util.List;

public record RespuestaPaginada<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {
}
