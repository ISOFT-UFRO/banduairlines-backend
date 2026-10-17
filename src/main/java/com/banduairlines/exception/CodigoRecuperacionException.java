package com.banduairlines.exception;

import com.banduairlines.service.cuenta.SolicitudesRecuperacionService.EstadoToken;

public class CodigoRecuperacionException
        extends RuntimeException {

    private final EstadoToken estado;

    public CodigoRecuperacionException(EstadoToken estado) {
        super("El codigo de recuperacion no puede utilizarse");
        this.estado = estado;
    }

    public EstadoToken getEstado() {
        return estado;
    }
}
