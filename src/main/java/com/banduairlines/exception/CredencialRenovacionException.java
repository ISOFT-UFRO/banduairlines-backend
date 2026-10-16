package com.banduairlines.exception;

import org.springframework.security.core.AuthenticationException;

public class CredencialRenovacionException
        extends AuthenticationException {

    private final boolean vencida;

    public CredencialRenovacionException(boolean vencida) {
        super(vencida
                ? "Credencial de renovacion vencida"
                : "Credencial de renovacion invalida");

        this.vencida = vencida;
    }

    public boolean estaVencida() {
        return vencida;
    }
}
