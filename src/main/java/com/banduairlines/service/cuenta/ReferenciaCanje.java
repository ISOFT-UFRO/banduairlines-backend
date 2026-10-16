package com.banduairlines.service.cuenta;

import com.banduairlines.exception.ValidacionException;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formato de la referencia con que se guarda un canje en RegistroPuntos:
 * {@code CANJE:<clave de idempotencia>:B<idBeneficio>}.
 * <p>
 * RegistroPuntos no tiene una columna para el beneficio, por eso se codifica en la referencia.
 * Si más adelante se agrega esa columna, solo cambia esta clase.
 */
public final class ReferenciaCanje {

    public static final int LARGO_MINIMO_CLAVE = 8;
    public static final int LARGO_MAXIMO_CLAVE = 64;

    private static final String PREFIJO = "CANJE:";
    private static final Pattern FORMATO_CLAVE = Pattern.compile("[A-Za-z0-9-]{8,64}");
    private static final Pattern FORMATO_REFERENCIA = Pattern.compile("CANJE:[A-Za-z0-9-]{8,64}:B(\\d{1,18})");

    private ReferenciaCanje() {
    }

    /** Valida la clave recibida en el encabezado Clave-Idempotencia y la devuelve sin espacios. */
    public static String validarClave(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new ValidacionException("El encabezado Clave-Idempotencia es obligatorio");
        }
        String limpia = clave.trim();
        if (!FORMATO_CLAVE.matcher(limpia).matches()) {
            throw new ValidacionException("La Clave-Idempotencia debe tener entre " + LARGO_MINIMO_CLAVE + " y "
                    + LARGO_MAXIMO_CLAVE + " caracteres, solo letras, números o guiones");
        }
        return limpia;
    }

    /** Inicio común de todas las referencias de canje hechas con una misma clave. */
    public static String prefijo(String clave) {
        return PREFIJO + clave + ":";
    }

    public static String crear(String clave, Long idBeneficio) {
        return prefijo(clave) + "B" + idBeneficio;
    }

    /** Id del beneficio canjeado, o vacío si la referencia no corresponde a un canje. */
    public static Optional<Long> idBeneficio(String referencia) {
        if (referencia == null) {
            return Optional.empty();
        }
        Matcher matcher = FORMATO_REFERENCIA.matcher(referencia);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(Long.valueOf(matcher.group(1)));
    }
}
