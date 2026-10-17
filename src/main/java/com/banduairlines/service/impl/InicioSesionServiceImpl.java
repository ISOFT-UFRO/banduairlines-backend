package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.RespuestaSesion;
import com.banduairlines.dto.cuenta.SolicitudInicioSesion;
import com.banduairlines.model.CuentaFidelizacion;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioAutenticado;
import com.banduairlines.service.cuenta.InicioSesionService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class InicioSesionServiceImpl implements InicioSesionService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public InicioSesionServiceImpl(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaSesion iniciarSesion(
            SolicitudInicioSesion solicitud) {

        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository.findByEmail(correo).orElseThrow(this::credencialesInvalidas);

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw credencialesInvalidas();
        }

        if (usuario.getPasswordHash() == null ||
                !passwordEncoder.matches(
                        solicitud.contrasena(),
                        usuario.getPasswordHash())) {
            throw credencialesInvalidas();
        }

        UsuarioAutenticado identidad =
                new UsuarioAutenticado(
                        usuario.getId(),
                        usuario.getEmail(),
                        usuario.getRol()
                );


        var credenciales = jwtService.emitirSesion(identidad, usuario.getPasswordHash());

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
                credenciales.acceso(),
                credenciales.renovacion(),
                "Bearer",
                jwtService.obtenerSegundosExpiracion(),
                datosUsuario
        );
    }

    private BadCredentialsException credencialesInvalidas() {
        return new BadCredentialsException(
                "Credenciales invalidas");
    }
}
