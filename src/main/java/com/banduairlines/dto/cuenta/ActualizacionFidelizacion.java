package com.banduairlines.dto.cuenta;

import com.banduairlines.model.enums.Nivel;
import java.math.BigDecimal;

public record ActualizacionFidelizacion(BigDecimal millasAcumuladas, long saldoPuntos,
                                        Nivel nivelActual, BigDecimal multiplicadorPuntos) { }
