package com.banduairlines.dto.checkin;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Size;

public record CancelacionUsuarioRequest(

        @JsonProperty("motivo_cancelacion")
        @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
        String motivoCancelacion
) {
}
