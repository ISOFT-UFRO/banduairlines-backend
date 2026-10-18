package com.banduairlines.dto.reportes;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record DashboardRequest(
        @NotNull(message = "La fecha desde es obligatoria")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate desde,
        @NotNull(message = "La fecha hasta es obligatoria")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate hasta
) {
    @AssertTrue(message = "El período debe estar ordenado y contener como máximo 366 días")
    public boolean isPeriodoValido() {
        return desde == null || hasta == null ||
                (!hasta.isBefore(desde) && hasta.isBefore(LocalDate.MAX)
                        && ChronoUnit.DAYS.between(desde, hasta) < 366);
    }
}
