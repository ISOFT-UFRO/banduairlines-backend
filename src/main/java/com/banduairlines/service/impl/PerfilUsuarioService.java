package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.RespuestaUsuario;
import com.banduairlines.dto.cuenta.SolicitudActualizacionPerfil;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.PerfilUsuarioRepository;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioActual;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class PerfilUsuarioService {
    private final UsuarioActual usuarioActual;
    private final UsuarioRepository usuarios;
    private final PerfilUsuarioRepository perfiles;
    private final Clock reloj;

    public PerfilUsuarioService(UsuarioActual usuarioActual, UsuarioRepository usuarios,
                                PerfilUsuarioRepository perfiles, Clock reloj) {
        this.usuarioActual = usuarioActual;
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public RespuestaUsuario consultarActual() {
        var identidad = usuarioActual.exigir();
        exigirRol(identidad.rol());
        var usuario = usuarios.findById(identidad.id()).orElseThrow(() ->
                new RecursoNoEncontradoException("La cuenta no existe").conCodigo("CUENTA_NO_ENCONTRADA"));
        exigirRol(usuario.getRol());
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new AuthenticationCredentialsNotFoundException("La cuenta no esta activa");
        }
        return MapeadorUsuario.aRespuesta(usuario);
    }

    @Transactional
    public RespuestaUsuario actualizarActual(SolicitudActualizacionPerfil solicitud) {
        var identidad = usuarioActual.exigir();
        exigirRol(identidad.rol());
        int actualizados = perfiles.actualizarPerfil(identidad.id(), solicitud.getNombres(),
                solicitud.getApellidos(), LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC));
        var usuario = usuarios.findById(identidad.id()).orElseThrow(() ->
                new RecursoNoEncontradoException("La cuenta no existe").conCodigo("CUENTA_NO_ENCONTRADA"));
        exigirRol(usuario.getRol());
        if (!Boolean.TRUE.equals(usuario.getActivo()) || actualizados != 1) {
            throw new AuthenticationCredentialsNotFoundException("La cuenta no esta activa");
        }
        if (solicitud.getTipoDocumento() != null) {
            usuario.setTipoDocumento(solicitud.getTipoDocumento());
            usuario.setNumeroDocumento(solicitud.getNumeroDocumento());
        }
        var respuesta = MapeadorUsuario.aRespuesta(usuario);
        return new RespuestaUsuario(respuesta.idUsuario(), respuesta.nombres(), respuesta.apellidos(),
                respuesta.correo(), respuesta.rol(), respuesta.rol() == RolUsuario.PASAJERO ? respuesta.nivel() : null);
    }

    private void exigirRol(RolUsuario rol) {
        if (rol != RolUsuario.PASAJERO && rol != RolUsuario.ADMINISTRADOR) {
            throw new AccessDeniedException("El rol no puede modificar el perfil");
        }
    }
}
