package com.banduairlines.service.cuenta;

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
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

@Service
public class JwtService {

    private static final String EMISOR = "banduairlines";
    private static final String AUDIENCIA = "banduairlines-api";
    private static final String TIPO_ACCESO = "acceso";
    private static final String SUJETO_SUPERADMIN = "superadmin";

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(
            @Value("${app.jwt.secret:}") String secreto,
            @Value("${app.jwt.expiration-ms:900000}") long expiracionMs,
            @Value("${app.security.enabled:false}") boolean seguridadActivada) {

        if (expiracionMs <= 0) {
            throw new IllegalArgumentException(
                    "La expiracion JWT debe ser positiva");
        }

        this.expiracionMs = expiracionMs;

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

    // Genera un JWT para un usuario ya registrado.
    public String generarTokenAcceso(UsuarioAutenticado usuario) {
        exigirClave();
        Objects.requireNonNull(usuario, "El usuario es obligatorio");

        // Verificamos que el usuario tenga un ID válido.
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

        // Obtenemos el ID como String para usarlo como subject.
        String sujeto = usuario.id().toString();

        // Normalizamos el correo.
        String correo = usuario.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Obtenemos el instante actual.
        Instant ahora = Instant.now();

        // Construimos y firmamos el JWT.
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
                .signWith(clave, SignatureAlgorithm.HS256)
                .compact();
    }


    // Verifica la firma, expiración y datos.
    public DatosJwt validarTokenAcceso(String token) {
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

        if (!TIPO_ACCESO.equals(claims.get("tipo", String.class))
                || claims.getIssuedAt() == null
                || claims.getExpiration() == null) {
            throw new JwtException("JWT de acceso invalido");
        }

        String sujeto = claims.getSubject();
        String correo = claims.get("email", String.class);
        String nombreRol = claims.get("rol", String.class);

        if (sujeto == null || sujeto.isBlank()
                || correo == null || correo.isBlank()
                || nombreRol == null) {
            throw new JwtException("Faltan datos de identidad");
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
                    "Identificador de usuario inválido", ex);
        }
        return new DatosJwt(id, correo, rol);
    }

    private void exigirClave() {
        if (clave == null) {
            throw new IllegalStateException(
                    "JWT_SECRET no esta configurada");
        }
    }

    public record DatosJwt(
            Long id,
            String email,
            RolUsuario rol
    ) {}
}
