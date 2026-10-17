package com.banduairlines.dto.checkin;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MotivoRestriccion {
    VENTANA_CERRADA,
    VUELO_EN_CURSO,
    TARIFA_NO_CANCELABLE,
    TARIFA_NO_REEMBOLSABLE,
    CHECKIN_REALIZADO,
    RESERVA_NO_CONFIRMADA;

    @JsonValue
    public String codigo() {
        return name().toLowerCase(Locale.ROOT);
    }
}
