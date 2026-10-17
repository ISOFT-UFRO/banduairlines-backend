package com.banduairlines.service.cuenta;

import com.banduairlines.dto.cuenta.RespuestaCanje;

public interface CanjeService {

    ResultadoCanje canjearActual(Long idBeneficio, String claveIdempotencia);

    /**
     * @param creado true si el canje se registró ahora; false si la clave ya se había usado
     *               para el mismo beneficio y se devuelve el canje original.
     */
    record ResultadoCanje(RespuestaCanje canje, boolean creado) {
    }
}
