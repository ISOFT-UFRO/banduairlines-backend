package com.banduairlines.dto.busqueda;

public record AeropuertoBusquedaResponse(Long id, String codigoIata, String nombre,
        String ciudad, String pais, String zonaHoraria) {
}
