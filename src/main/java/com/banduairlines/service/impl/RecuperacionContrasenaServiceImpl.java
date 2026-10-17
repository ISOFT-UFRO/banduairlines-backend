package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.SolicitudNuevaContrasena;
import com.banduairlines.exception.ServicioExternoException;
import com.banduairlines.exception.CodigoRecuperacionException;
import com.banduairlines.service.cuenta.RecuperacionContrasenaService;
import com.banduairlines.service.cuenta.ServicioCorreoRecuperacion;
import com.banduairlines.service.cuenta.SolicitudesRecuperacionService;
import com.banduairlines.service.cuenta.SolicitudesRecuperacionService.EstadoToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class RecuperacionContrasenaServiceImpl
        implements RecuperacionContrasenaService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RecuperacionContrasenaServiceImpl.class
            );

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SolicitudesRecuperacionService solicitudes;
    private final ServicioCorreoRecuperacion servicioCorreo;
    private final String urlBase;

    public RecuperacionContrasenaServiceImpl(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            SolicitudesRecuperacionService solicitudes,
            ServicioCorreoRecuperacion servicioCorreo,
            @Value("${app.cuenta.recuperacion.url-base}")
            String urlBase) {

        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.solicitudes = solicitudes;
        this.servicioCorreo = servicioCorreo;
        this.urlBase = urlBase;
    }

    @Override
    public void solicitarRecuperacion(String correo) {

        String correoNormalizado = correo
                .trim()
                .toLowerCase(Locale.ROOT);

        List<CuentaRecuperacion> cuentas = jdbcTemplate.query(
                """
                SELECT id, email
                FROM usuarios
                WHERE LOWER(email) = ?
                  AND activo = TRUE
                """,
                (rs, numeroFila) -> new CuentaRecuperacion(
                        rs.getLong("id"),
                        rs.getString("email")
                ),
                correoNormalizado
        );

        if (cuentas.isEmpty()) {
            return;
        }

        CuentaRecuperacion cuenta = cuentas.get(0);

        SolicitudesRecuperacionService.TokenEmitido emitido =
                solicitudes.crearSolicitud(cuenta.idUsuario());

        String separador = urlBase.contains("?") ? "&" : "?";

        String enlace = urlBase
                + separador
                + "codigoRecuperacion="
                + URLEncoder.encode(
                emitido.token(),
                StandardCharsets.UTF_8
        );

        try {
            servicioCorreo.enviarEnlaceRecuperacion(
                    cuenta.correo(),
                    enlace,
                    emitido.fechaExpiracion()
            );

        } catch (ServicioExternoException ex) {

            log.warn(
                    "No fue posible enviar un correo de recuperacion"
            );
        }
    }


    @Override
    public void validarRecuperacion(String codigoRecuperacion) {

        EstadoToken estado =
                solicitudes.estadoToken(codigoRecuperacion);

        exigirCodigoValido(estado);
    }


    @Override
    public void restablecerContrasena(
            String codigoRecuperacion,
            SolicitudNuevaContrasena solicitud) {

        if (!solicitud.nuevaContrasena().equals(
                solicitud.confirmacionContrasena())) {

            throw new IllegalArgumentException(
                    "La nueva contrasena y su confirmacion no coinciden"
            );
        }

        EstadoToken estado =
                solicitudes.estadoToken(codigoRecuperacion);

        exigirCodigoValido(estado);

        Optional<Long> idUsuario =
                solicitudes.consumirSolicitud(codigoRecuperacion);

        if (idUsuario.isEmpty()) {

            EstadoToken estadoActual =
                    solicitudes.estadoToken(codigoRecuperacion);

            throw new CodigoRecuperacionException(
                    estadoActual == EstadoToken.VALIDO
                            ? EstadoToken.INVALIDO
                            : estadoActual
            );
        }

        String nuevoHash = passwordEncoder.encode(
                solicitud.nuevaContrasena()
        );

        int filasActualizadas = jdbcTemplate.update(
                """
                UPDATE usuarios
                SET password_hash = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND activo = TRUE
                """,
                nuevoHash,
                idUsuario.get()
        );

        if (filasActualizadas != 1) {
            throw new CodigoRecuperacionException(
                    EstadoToken.INVALIDO
            );
        }

    }

    private void exigirCodigoValido(EstadoToken estado) {

        if (estado != EstadoToken.VALIDO) {
            throw new CodigoRecuperacionException(estado);
        }
    }

    private record CuentaRecuperacion(
            Long idUsuario,
            String correo
    ) {
    }
}
