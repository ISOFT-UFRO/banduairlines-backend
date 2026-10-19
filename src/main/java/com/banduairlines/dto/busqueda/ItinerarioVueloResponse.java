package com.banduairlines.dto.busqueda;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
public record ItinerarioVueloResponse(
        String tipoItinerario,
        int cantidadEscalas,
        String aeropuertoEscalaIata,
        String ciudadEscala,
        Long tiempoEscalaMinutos,
        LocalDateTime horarioSalida,
        LocalDateTime horarioLlegada,
        long duracionTotalMinutos,
        List<TramoVueloResponse> tramos,
        List<TarifaItinerarioResponse> tarifas
) {}
