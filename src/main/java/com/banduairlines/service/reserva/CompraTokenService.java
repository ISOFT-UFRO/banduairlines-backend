package com.banduairlines.service.reserva;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Credencial de compra del invitado (header X-Compra-Token)
 * Es un HMAC del id de la reserva, asi que no necesita columna en la tabla reservas:
 * POST /reservas lo entrega con generar(id) y el resto de los endpoints lo verifica con esValido
 * 
 * La clave es propia de RESERVA (app.reserva.compra-token-secret, o la variable de entorno
 * APP_RESERVA_COMPRA_TOKEN_SECRET) y no depende del secreto JWT de CUENTAS. Si no esta configurada,
 * se genera una clave aleatoria al arrancar: sirve en desarrollo, pero los tokens emitidos dejan de valer
 * cuando la aplicacion se reinicia, asi que en servidor conviene configurarla
 */
@Component
public class CompraTokenService {

    private static final Logger log = LoggerFactory.getLogger(CompraTokenService.class);
    private static final String ALGORITMO = "HmacSHA256";
    private static final int BYTES_CLAVE_ALEATORIA = 32;

    private final SecretKeySpec clave;

    public CompraTokenService(@Value("${app.reserva.compra-token-secret:}") String secreto) {
        byte[] bytes;
        if (secreto == null || secreto.isBlank()) {
            log.warn("app.reserva.compra-token-secret no está configurado: se usa una clave aleatoria. "
                    + "Los X-Compra-Token emitidos dejarán de valer al reiniciar la aplicación.");
            bytes = new byte[BYTES_CLAVE_ALEATORIA];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = secreto.getBytes(StandardCharsets.UTF_8);
        }
        this.clave = new SecretKeySpec(bytes, ALGORITMO);
    }

    public String generar(Long reservaId) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(clave);
            byte[] firma = mac.doFinal(("reserva:" + reservaId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(firma);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo generar el token de compra", e);
        }
    }

    public boolean esValido(Long reservaId, String token) {
        if (reservaId == null || token == null || token.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                generar(reservaId).getBytes(StandardCharsets.UTF_8),
                token.trim().getBytes(StandardCharsets.UTF_8));
    }
}