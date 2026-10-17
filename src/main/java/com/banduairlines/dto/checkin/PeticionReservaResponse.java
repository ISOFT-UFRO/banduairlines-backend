package com.banduairlines.dto.checkin;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PeticionReservaResponse(
    @JsonProperty("pais_destino") String paisDestino,
    @JsonProperty("codigo_pnr") String codigoPnr,
    @JsonProperty("fecha_salida") String fechaSalida,   // yyyy-MM-dd
    @JsonProperty("hora_salida") String horaSalida,     // HH:mm
    @JsonProperty("check_in") String checkIn,
    String nombre,
    String apellido,
    String correo
) {}