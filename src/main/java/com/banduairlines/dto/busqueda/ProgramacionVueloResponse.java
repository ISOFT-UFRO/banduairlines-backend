package com.banduairlines.dto.busqueda;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ProgramacionVueloResponse(
        Long id,
        String codigoVuelo,
        Long rutaId,
        String origenCodigoIata,
        String destinoCodigoIata,
        Long aeronaveId,
        String aeronaveMatricula,
        String aeronaveModelo,
        LocalDateTime salidaProgramada,
        LocalDateTime llegadaProgramada,
        long duracionMinutos,
        String estado,
        LocalDateTime createdAt
) {}