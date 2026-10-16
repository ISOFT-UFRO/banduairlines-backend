package com.banduairlines.service.cuenta;

/**
 * Códigos de error propios de la consulta de fidelización, el catálogo de beneficios y los canjes.
 * Se usan con .conCodigo(...) sobre las excepciones comunes.
 */
public final class CodigosFidelizacion {

    private CodigosFidelizacion() {
    }

    public static final String BENEFICIO_NO_ENCONTRADO = "BENEFICIO_NO_ENCONTRADO";
    public static final String PUNTOS_INSUFICIENTES = "PUNTOS_INSUFICIENTES";
    public static final String NIVEL_INSUFICIENTE = "NIVEL_INSUFICIENTE";
    public static final String BENEFICIO_AGOTADO = "BENEFICIO_AGOTADO";
    public static final String CANJE_DUPLICADO = "CANJE_DUPLICADO";
    /** Lo emite el cálculo de millas (ActualizacionFidelizacionService) y se propaga tal cual. */
    public static final String MILLAS_NO_DISPONIBLES = "MILLAS_NO_DISPONIBLES";
}
