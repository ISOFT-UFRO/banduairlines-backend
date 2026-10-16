package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.RespuestaSesion;
import com.banduairlines.dto.cuenta.SolicitudRenovacion;
import com.banduairlines.exception.CredencialRenovacionException;
import com.banduairlines.model.CuentaFidelizacion;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioAutenticado;
import com.banduairlines.service.cuenta.RenovacionSesionService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RenovacionSesionServiceImpl
        implements RenovacionSesionService {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public RenovacionSesionServiceImpl(
            JwtService jwtService,
            UsuarioRepository usuarioRepository) {

        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaSesion renovarSesion(
            SolicitudRenovacion solicitud) {

        JwtService.DatosJwt datos;

        try {
            datos = jwtService.validarTokenRenovacion(
                    solicitud.credencialRenovacion()
            );

        } catch (ExpiredJwtException ex) {
            throw new CredencialRenovacionException(true);

        } catch (JwtException | IllegalArgumentException ex) {
            throw new CredencialRenovacionException(false);
        }

        Usuario usuario = usuarioRepository
                .findById(datos.id())
                .orElseThrow(() ->
                        new CredencialRenovacionException(false));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new CredencialRenovacionException(false);
        }

        if (usuario.getEmail() == null ||
                usuario.getRol() == null ||
                !usuario.getEmail().trim()
                        .equalsIgnoreCase(datos.email()) ||
                usuario.getRol() != datos.rol()) {

            throw new CredencialRenovacionException(false);
        }

        try {
            jwtService.exigirHuellaVigente(
                    datos,
                    usuario.getPasswordHash()
            );

        } catch (JwtException | IllegalArgumentException ex) {
            throw new CredencialRenovacionException(false);
        }

        UsuarioAutenticado identidad =
                new UsuarioAutenticado(
                        usuario.getId(),
                        usuario.getEmail(),
                        usuario.getRol()
                );

        String credencialAcceso =
                jwtService.generarTokenAcceso(identidad, usuario.getPasswordHash());

        String credencialRenovacion =
                jwtService.generarTokenRenovacion(identidad, usuario.getPasswordHash());

        Nivel nivel = null;

        if (usuario.getRol() == RolUsuario.PASAJERO) {

            CuentaFidelizacion cuenta =
                    usuario.getCuentaFidelizacion();

            if (cuenta != null) {
                nivel = cuenta.getNivel();
            }
        }

        RespuestaSesion.DatosUsuario datosUsuario =
                new RespuestaSesion.DatosUsuario(
                        usuario.getId(),
                        usuario.getNombre(),
                        usuario.getApellido(),
                        usuario.getEmail(),
                        usuario.getRol(),
                        nivel
                );

        return new RespuestaSesion(
                credencialAcceso,
                credencialRenovacion,
                "Bearer",
                jwtService.obtenerSegundosExpiracion(),
                datosUsuario
        );
    }
}
