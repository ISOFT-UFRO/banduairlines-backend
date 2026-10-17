package com.banduairlines.dto.checkin;

public record CheckinNoRegistradoDto(
        String pnr,
        String apellidos,
        CheckinPasajeroDto[] pasajeros
) {
}
