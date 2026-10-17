package com.banduairlines.service.impl;

import com.banduairlines.security.UsuarioAutenticado;
import com.banduairlines.model.enums.RolUsuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.time.Instant;
import java.util.UUID;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

@Service
public class JwtService {

    private static final String EMISOR = "banduairlines";
    private static final String AUDIENCIA = "banduairlines-api";
    private static final String TIPO_ACCESO = "acceso";
    private static final String TIPO_RENOVACION = "renovacion";

    private final SecretKey clave;
    private final long expiracionMs;
    private final long expiracionRenovacionMs;

    public JwtService(
            @Value("${app.jwt.secret:}") String secreto,
            @Value("${app.jwt.expiration-ms:900000}") long expiracionMs,
            @Value("${app.jwt.refresh-expiration-ms:604800000}")
            long expiracionRenovacionMs,
            @Value("${app.security.enabled:false}")
            boolean seguridadActivada) {

        if (expiracionMs <= 0 || expiracionRenovacionMs <= 0) {
            throw new IllegalArgumentException(
                    "Las duraciones JWT deben ser positivas");
        }

        this.expiracionMs = expiracionMs;
        this.expiracionRenovacionMs = expiracionRenovacionMs;

        if (secreto == null || secreto.isBlank()) {
            if (seguridadActivada) {
                throw new IllegalStateException(
                        "JWT_SECRET es obligatorio con seguridad activada");
            }

            this.clave = null;
            return;
        }

        byte[] bytes = Decoders.BASE64.decode(secreto.trim());

        if (bytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET debe contener al menos 32 bytes");
        }

        this.clave = Keys.hmacShaKeyFor(bytes);
    }


    public String generarTokenAcceso(
            UsuarioAutenticado usuario,
            String hashContrasena) {
        exigirClave();
        Objects.requireNonNull(usuario, "El usuario es obligatorio");

        if (usuario.id() == null || usuario.id() <= 0) {
            throw new IllegalArgumentException(
                    "El usuario debe tener un ID válido");
        }

        // Verificamos que exista un rol.
        if (usuario.rol() == null) {
            throw new IllegalArgumentException(
                    "El usuario debe tener un rol");
        }

        // Verificamos que exista un correo.
        if (usuario.email() == null || usuario.email().isBlank()) {
            throw new IllegalArgumentException(
                    "El correo es obligatorio");
        }

        String sujeto = usuario.id().toString();

        String correo = usuario.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        Instant ahora = Instant.now();

        return Jwts.builder()
                .setSubject(sujeto)
                .setIssuer(EMISOR)
                .setAudience(AUDIENCIA)
                .setIssuedAt(Date.from(ahora))
                .setExpiration(Date.from(
                        ahora.plusMillis(expiracionMs)))
                .claim("email", correo)
                .claim("rol", usuario.rol().name())
                .claim("tipo", TIPO_ACCESO)
                .claim("huellaCredenciales",
                        generarHuellaCredenciales(
                                usuario.id(), hashContrasena))
                .signWith(clave, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generarTokenRenovacion(
            UsuarioAutenticado usuario,
            String hashContrasena) {
        exigirClave();
        Objects.requireNonNull(usuario, "El usuario es obligatorio");

        if (usuario.id() == null || usuario.id() <= 0
                || usuario.rol() == null
                || usuario.email() == null
                || usuario.email().isBlank()) {

            throw new IllegalArgumentException(
                    "Los datos del usuario no son validos");
        }

        String correo = usuario.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        Instant ahora = Instant.now();

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(usuario.id().toString())
                .setIssuer(EMISOR)
                .setAudience(AUDIENCIA)
                .setIssuedAt(Date.from(ahora))
                .setExpiration(Date.from(
                        ahora.plusMillis(expiracionRenovacionMs)))
                .claim("email", correo)
                .claim("rol", usuario.rol().name())
                .claim("tipo", TIPO_RENOVACION)
                .claim("huellaCredenciales",
                        generarHuellaCredenciales(
                                usuario.id(), hashContrasena))
                .signWith(clave, SignatureAlgorithm.HS256)
                .compact();
    }

    private DatosJwt validarToken(
            String token,
            String tipoEsperado) {

        exigirClave();

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT vacio");
        }

        Jws<Claims> resultado = Jwts.parserBuilder()
                .setSigningKey(clave)
                .requireIssuer(EMISOR)
                .requireAudience(AUDIENCIA)
                .build()
                .parseClaimsJws(token);

        if (!"HS256".equals(
                resultado.getHeader().getAlgorithm())) {
            throw new JwtException("Algoritmo JWT no permitido");
        }

        Claims claims = resultado.getBody();

        if (!tipoEsperado.equals(
                claims.get("tipo", String.class))) {
            throw new JwtException("Tipo de JWT incorrecto");
        }

        if (claims.getIssuedAt() == null ||
                claims.getExpiration() == null ||
                claims.getIssuedAt().after(new Date())) {
            throw new JwtException("Fechas JWT invalidas");
        }

        if (TIPO_RENOVACION.equals(tipoEsperado) &&
                (claims.getId() == null ||
                        claims.getId().isBlank())) {
            throw new JwtException(
                    "JWT de renovacion sin identificador");
        }

        String sujeto = claims.getSubject();
        String correo = claims.get("email", String.class);
        String nombreRol = claims.get("rol", String.class);
        String huellaCredenciales = claims.get("huellaCredenciales", String.class);

        if (sujeto == null || sujeto.isBlank() ||
                correo == null || correo.isBlank() ||
                nombreRol == null ||
                huellaCredenciales == null ||
                huellaCredenciales.isBlank()) {

            throw new JwtException(
                    "Faltan datos de identidad o credenciales");
        }

        RolUsuario rol;

        try {
            rol = RolUsuario.valueOf(nombreRol);
        } catch (IllegalArgumentException ex) {
            throw new JwtException("Rol JWT desconocido", ex);
        }

        Long id;

        try {
            id = Long.valueOf(sujeto);

            if (id <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException ex) {
            throw new JwtException(
                    "Identificador de usuario invalido", ex);
        }

        return new DatosJwt(id, correo, rol, huellaCredenciales);    }

    public DatosJwt validarTokenAcceso(String token) {
        return validarToken(token, TIPO_ACCESO);
    }

    public DatosJwt validarTokenRenovacion(String token) {
        return validarToken(token, TIPO_RENOVACION);
    }

    private void exigirClave() {
        if (clave == null) {
            throw new IllegalStateException(
                    "JWT_SECRET no esta configurada");
        }
    }

    public long obtenerSegundosExpiracion() {
        return expiracionMs / 1000;
    }

    private String generarHuellaCredenciales(
            Long idUsuario,
            String hashContrasena) {

        exigirClave();

        if (idUsuario == null || idUsuario <= 0 ||
                hashContrasena == null ||
                hashContrasena.isBlank()) {

            throw new IllegalArgumentException(
                    "No se puede generar la huella de credenciales");
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(clave);

            String contenido =
                    "banduairlines:huella-credenciales:v1:"
                            + idUsuario + ":" + hashContrasena;

            byte[] huella = mac.doFinal(
                    contenido.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(huella);

        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "No se pudo generar la huella de credenciales",
                    ex);
        }
    }

    public void exigirHuellaVigente(
            DatosJwt datos,
            String hashContrasenaActual) {

        Objects.requireNonNull(datos, "Datos JWT obligatorios");

        if (datos.huellaCredenciales() == null ||
                datos.huellaCredenciales().isBlank() ||
                hashContrasenaActual == null ||
                hashContrasenaActual.isBlank()) {

            throw new JwtException(
                    "No se puede validar el estado de las credenciales");
        }

        String huellaActual = generarHuellaCredenciales(
                datos.id(),
                hashContrasenaActual
        );

        boolean coincide = MessageDigest.isEqual(
                huellaActual.getBytes(StandardCharsets.US_ASCII),
                datos.huellaCredenciales()
                        .getBytes(StandardCharsets.US_ASCII)
        );

        if (!coincide) {
            throw new JwtException(
                    "La credencial fue invalidada por un cambio de contraseña");
        }
    }


    public record DatosJwt(
            Long id,
            String email,
            RolUsuario rol,
            String huellaCredenciales
    ) {}
}
