package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.SolicitudEliminacionCuenta;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.cuenta.CambioCorreoService;
import com.banduairlines.service.cuenta.ServicioCorreo;
import com.banduairlines.service.cuenta.SolicitudesRecuperacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.UUID;

@Service
public class EliminacionCuentaService {
    private static final Logger log = LoggerFactory.getLogger(EliminacionCuentaService.class);
    private final UsuarioRepository usuarios;
    private final UsuarioActual actual;
    private final PasswordEncoder contrasenas;
    private final JwtService sesiones;
    private final SolicitudesRecuperacionService recuperaciones;
    private final CambioCorreoService cambiosCorreo;
    private final ServicioCorreo correo;

    public EliminacionCuentaService(UsuarioRepository usuarios, UsuarioActual actual, PasswordEncoder contrasenas,
            JwtService sesiones, SolicitudesRecuperacionService recuperaciones,
            CambioCorreoService cambiosCorreo, ServicioCorreo correo) {
        this.usuarios = usuarios;
        this.actual = actual;
        this.contrasenas = contrasenas;
        this.sesiones = sesiones;
        this.recuperaciones = recuperaciones;
        this.cambiosCorreo = cambiosCorreo;
        this.correo = correo;
    }

    @Transactional
    public void eliminarActual(SolicitudEliminacionCuenta solicitud) {
        Long id = actual.exigirId();
        var usuario = usuarios.buscarParaActualizar(id).orElseThrow(() ->
                new AuthenticationCredentialsNotFoundException("La cuenta no esta disponible"));
        if (!Boolean.TRUE.equals(usuario.getActivo()))
            throw new AuthenticationCredentialsNotFoundException("La cuenta no esta activa");
        if (usuario.getRol() != RolUsuario.PASAJERO && usuario.getRol() != RolUsuario.ADMINISTRADOR)
            throw new AccessDeniedException("El rol no puede eliminar su cuenta");
        if (!contrasenas.matches(solicitud.contrasenaActual(), usuario.getPasswordHash()))
            throw new ReglaNegocioException("La contrasena actual es incorrecta")
                    .conCodigo("CONTRASENA_ACTUAL_INCORRECTA");
        String destinatario = usuario.getEmail();
        usuario.setActivo(false);
        usuario.setEmail("eliminada-" + UUID.randomUUID() + "@cuentas.invalid");
        usuario.setPasswordHash(contrasenas.encode(UUID.randomUUID().toString()));
        usuario.setNombre("Cuenta eliminada");
        usuario.setApellido("Eliminada");
        usuario.setTipoDocumento(null);
        usuario.setNumeroDocumento(null);
        usuario.setFechaNacimiento(null);
        usuario.setTelefono(null);
        usuarios.saveAndFlush(usuario);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sesiones.revocarTodas(id);
                recuperaciones.invalidarUsuario(id);
                cambiosCorreo.invalidarUsuario(id);
                try {
                    correo.notificarEliminacionCuenta(destinatario);
                } catch (RuntimeException ex) {
                    log.warn("La cuenta fue eliminada, pero no se pudo enviar la notificacion por correo");
                }
            }
        });
    }
}
