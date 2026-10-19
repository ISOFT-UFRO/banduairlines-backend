package com.banduairlines.exception;

// se creo exception para el correcto funcionamienot del endpoint 14
public class DemasiadosIntentosException extends RuntimeException {

    private final long reintentarEnSegundos;

    public DemasiadosIntentosException(long reintentarEnSegundos) {
        super("Demasiados intentos, inténtalo nuevamente más tarde");
        this.reintentarEnSegundos = reintentarEnSegundos;
    }

    public long getReintentarEnSegundos() {
        return reintentarEnSegundos;
    }
}
