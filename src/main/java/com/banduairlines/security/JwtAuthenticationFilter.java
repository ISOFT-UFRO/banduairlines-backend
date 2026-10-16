package com.banduairlines.security;

import com.banduairlines.model.Usuario;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.service.cuenta.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UsuarioRepository usuarioRepository) {

        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String encabezado = request.getHeader(
                HttpHeaders.AUTHORIZATION);

        //Si no hay Bearer Token, continuar sin autenticar.
        if (encabezado == null ||
                !encabezado.startsWith("Bearer ")) {

            chain.doFilter(request, response);
            return;
        }

        //Extraer el JWT.
        String token = encabezado.substring(7).trim();
        try {
            // Evitar reemplazar una autenticacion existente.
            if (SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                autenticarUsuario(token);
            }

        } catch (JwtException | IllegalArgumentException ex) {
            // No autenticar si el token es invalido.
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(request, response);
    }

    private void autenticarUsuario(String token) {

        // 1. Validar firma, expiracion y contenido del JWT.
        JwtService.DatosJwt datos = jwtService.validarTokenAcceso(token);

        // 2. Buscar al usuario.
        Usuario usuario = usuarioRepository
                .findById(datos.id())
                .orElse(null);

        // 3. Comprobar existencia y estado de la cuenta.
        if (usuario == null ||
                !Boolean.TRUE.equals(usuario.getActivo())) {
            return;
        }

        // 4. Comprobar que el correo y rol sigan vigentes.
        if (usuario.getEmail() == null ||
                usuario.getRol() == null ||
                !usuario.getEmail().trim()
                        .equalsIgnoreCase(datos.email()) ||
                usuario.getRol() != datos.rol()) {

            return;
        }

        // 5. Crear la identidad autenticada.
        UsuarioAutenticado principal =
                new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getRol());

        // 6. Convertir el rol a una autoridad de Spring.
        var autoridades = List.of(
                new SimpleGrantedAuthority(
                        "ROLE_" + usuario.getRol().name()
                )
        );

        // 7. Crear el objeto de autenticacion.
        var autenticacion =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        autoridades
                );

        // 8. Registrar la identidad en Spring Security.
        SecurityContextHolder.getContext()
                .setAuthentication(autenticacion);
    }
}
