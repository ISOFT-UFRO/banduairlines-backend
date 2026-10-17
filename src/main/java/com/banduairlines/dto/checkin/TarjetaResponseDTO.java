package com.banduairlines.dto.checkin;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TarjetaResponseDTO(
        @JsonProperty("codigo_pnr") String codigoPnr,
        @JsonProperty("ciudad_origen") String ciudadOrigen,
        @JsonProperty("ciudad_destino") String ciudadDestino,
        @JsonProperty("nombre") String nombre,
        @JsonProperty("apellido") String apellido,
        @JsonProperty("pais_origen") String paisOrigen,
        @JsonProperty("pais_destino") String paisDestino,
        @JsonProperty("codigo_aerolinea") String codigoAerolinea,
        @JsonProperty("fecha_salida") LocalDate fechaSalida,
        @JsonProperty("hora_sala") LocalTime horaSala,
        @JsonProperty("hora_cierre_embarque") LocalTime horaCierreEmbarque,
        @JsonProperty("puerta") String puerta,
        @JsonProperty("asiento") String asiento,
        @JsonProperty("numero_ticket") String numeroTicket
) {}