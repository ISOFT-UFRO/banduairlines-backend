package com.banduairlines.service.reserva;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

//endpoint 13
//Clave compartida entre módulos para las rutas /internas (cabecera X-Service-Key).
//Si app.internal.service-key no está configurada, ninguna petición HTTP pasa.

@Component
public class ClaveServicioInterna {

    private final String clave;

    public ClaveServicioInterna(@Value("${app.internal.service-key:}") String clave) {
        this.clave = clave;
    }

    // Lanza BadCredentialsException (401 NO_AUTENTICADO) si la contraseña falta o no coincide
    public void exigir(String claveRecibida) {
        boolean valida = !clave.isBlank()
                && claveRecibida != null
                && MessageDigest.isEqual(
                        clave.getBytes(StandardCharsets.UTF_8),
                        claveRecibida.getBytes(StandardCharsets.UTF_8));
        if (!valida) {
            throw new BadCredentialsException("Clave de servicio ausente o inválida");
        }
    }
}
