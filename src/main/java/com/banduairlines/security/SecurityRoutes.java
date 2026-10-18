package com.banduairlines.security;

public final class SecurityRoutes {

    private SecurityRoutes() {
    }

    public static final String API = "/api/v1";

    public static final String[] PUBLICAS = {
            API + "/health",
            "/actuator/health",
            "/actuator/health/**",
            API + "/pagos/notificaciones",
            API + "/checkin/invitado/**",
            API + "/reservas/*/servicios",
            API + "/reservas/*/promocion",
            API + "/reservas/*/pago",
            API + "/reservas/*/cancelacion"
    };

    public static final String[] PUBLICAS_GET = {
            API + "/busqueda/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    public static final String[] AUTH_PUBLICAS_POST = {
            API + "/auth/registro",
            API + "/auth/inicio-sesion",
            API + "/auth/renovacion",
            API + "/auth/recuperaciones"
    };

    public static final String[] AUTH_CODIGOS_RECUPERACION = {
            API + "/auth/recuperaciones/*"
    };

    public static final String AUTH_CIERRE_SESION =
            API + "/auth/cierre-sesion";

    public static final String[] SOLO_PASAJERO = {
            API + "/cuentas/actual/fidelizacion",
            API + "/cuentas/actual/fidelizacion/**",
            API + "/fidelizacion/**"
    };

    public static final String[] CUENTA_ACTUAL = {
            API + "/cuentas/actual",
            API + "/cuentas/actual/**"
    };

    public static final String[] SOLO_ADMIN = {
            API + "/admin/**"
    };
}
