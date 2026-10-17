package com.banduairlines.dto.admin;

public record RutaResponse(
        Long id,
        Long origenId,
        String origenCodigoIata,
        String origenCiudad,
        Long destinoId,
        String destinoCodigoIata,
        String destinoCiudad,
        Integer distanciaKm,
        Integer duracionMin,
        Boolean activa) {

}
