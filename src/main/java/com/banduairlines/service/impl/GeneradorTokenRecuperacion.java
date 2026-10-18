package com.banduairlines.service.impl;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Component
public class GeneradorTokenRecuperacion {

    private static final int TAMANO_TOKEN_BYTES = 32;

    private static final Pattern FORMATO_TOKEN =
            Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private final SecureRandom secureRandom = new SecureRandom();

    public String generarToken() {

        byte[] bytes = new byte[TAMANO_TOKEN_BYTES];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public String calcularHash(String token) {

        if (token == null ||
                !FORMATO_TOKEN.matcher(token).matches()) {

            throw new IllegalArgumentException(
                    "Formato de token de recuperacion invalido"
            );
        }

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.US_ASCII)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 no esta disponible", ex
            );
        }
    }
}
