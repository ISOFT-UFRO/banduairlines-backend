package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.RespuestaPaginada;
import com.banduairlines.dto.cuenta.RespuestaUsuario;
import com.banduairlines.dto.cuenta.SolicitudCreacionAdministrador;
import com.banduairlines.exception.CodigosError;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.cuenta.AdministracionCuentaService;
import com.banduairlines.service.cuenta.CambioCorreoService;
import com.banduairlines.service.cuenta.SolicitudesRecuperacionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AdministracionCuentaServiceImpl implements AdministracionCuentaService {

    private static final int TAMANO_MAXIMO_PAGINA = 100;

    private final UsuarioRepository usuarios;
    private final UsuarioActual usuarioActual;
    private final PasswordEncoder contrasenas;
    private final JwtService sesiones;
    private final SolicitudesRecuperacionService recuperaciones;
    private final CambioCorreoService cambiosCorreo;
    private final String correoPrimerAdministrador;

    public AdministracionCuentaServiceImpl(
            UsuarioRepository usuarios,
            UsuarioActual usuarioActual,
            PasswordEncoder contrasenas,
            JwtService sesiones,
            SolicitudesRecuperacionService recuperaciones,
            CambioCorreoService cambiosCorreo,
            @Value("${app.admin.email:}") String correoPrimerAdministrador
    ) {
        this.usuarios = usuarios;
        this.usuarioActual = usuarioActual;
        this.contrasenas = contrasenas;
        this.sesiones = sesiones;
        this.recuperaciones = recuperaciones;
        this.cambiosCorreo = cambiosCorreo;
        this.correoPrimerAdministrador = normalizarCorreo(correoPrimerAdministrador);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPaginada<RespuestaUsuario> listarAdministradores(int pagina, int tamano) {
        exigirPrimerAdministrador();
        validarPaginacion(pagina, tamano);
        List<RespuestaUsuario> administradores = usuarios.findAll().stream()
                .filter(this::esAdministradorActivo)
                .sorted(Comparator.comparing(Usuario::getId))
                .map(MapeadorUsuario::aRespuesta)
                .toList();
        long total = administradores.size();
        long desplazamiento = (long) pagina * tamano;
        int desde = (int) Math.min(desplazamiento, total);
        int hasta = (int) Math.min(desplazamiento + tamano, total);
        int totalPaginas = total == 0 ? 0 : (int) ((total + tamano - 1) / tamano);
        return new RespuestaPaginada<>(administradores.subList(desde, hasta), pagina, tamano, total, totalPaginas);
    }

    @Override
    @Transactional
    public RespuestaUsuario crearAdministrador(SolicitudCreacionAdministrador solicitud) {
        exigirPrimerAdministrador();
        String correo = normalizarCorreo(solicitud.correo());
        if (usuarios.existsByEmail(correo)) {
            throw correoYaRegistrado();
        }
        Usuario administrador = Usuario.builder()
                .nombre(solicitud.nombres().trim())
                .apellido(solicitud.apellidos().trim())
                .email(correo)
                .passwordHash(contrasenas.encode(solicitud.contrasenaInicial()))
                .rol(RolUsuario.ADMINISTRADOR)
                .activo(true)
                .build();
        try {
            return MapeadorUsuario.aRespuesta(usuarios.saveAndFlush(administrador));
        } catch (DataIntegrityViolationException ex) {
            throw correoYaRegistrado();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaUsuario obtenerAdministrador(Long idAdministrador) {
        exigirPrimerAdministrador();
        validarId(idAdministrador);
        return MapeadorUsuario.aRespuesta(buscarAdministradorActivo(idAdministrador));
    }

    @Override
    @Transactional
    public void eliminarAdministrador(Long idAdministrador) {
        Usuario primero = exigirPrimerAdministrador();
        validarId(idAdministrador);
        if (primero.getId().equals(idAdministrador)) {
            throw new AccessDeniedException("El primer administrador no puede eliminarse desde esta operacion");
        }
        Usuario administrador = usuarios.buscarParaActualizar(idAdministrador)
                .filter(this::esAdministradorActivo)
                .orElseThrow(this::administradorNoEncontrado);
        administrador.setActivo(false);
        administrador.setEmail("eliminada-" + UUID.randomUUID() + "@cuentas.invalid");
        administrador.setPasswordHash(contrasenas.encode(UUID.randomUUID().toString()));
        administrador.setNombre("Cuenta eliminada");
        administrador.setApellido("Eliminada");
        administrador.setTipoDocumento(null);
        administrador.setNumeroDocumento(null);
        administrador.setFechaNacimiento(null);
        administrador.setTelefono(null);
        usuarios.saveAndFlush(administrador);
        registrarInvalidacionTrasConfirmar(idAdministrador);
    }

    private Usuario exigirPrimerAdministrador() {
        if (correoPrimerAdministrador.isBlank()) {
            throw new AccessDeniedException("El primer administrador no esta configurado");
        }
        var identidad = usuarioActual.exigir();
        Usuario usuario = usuarios.findById(identidad.id())
                .filter(this::esAdministradorActivo)
                .orElseThrow(() -> new AccessDeniedException("La cuenta no administra usuarios"));
        if (!correoPrimerAdministrador.equals(usuario.getEmail())) {
            throw new AccessDeniedException("Solo el primer administrador puede gestionar administradores");
        }
        return usuario;
    }

    private Usuario buscarAdministradorActivo(Long idAdministrador) {
        return usuarios.findById(idAdministrador)
                .filter(this::esAdministradorActivo)
                .orElseThrow(this::administradorNoEncontrado);
    }

    private boolean esAdministradorActivo(Usuario usuario) {
        return usuario.getRol() == RolUsuario.ADMINISTRADOR && Boolean.TRUE.equals(usuario.getActivo());
    }

    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < 0) {
            throw new ValidacionException("La pagina no puede ser negativa");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO_PAGINA) {
            throw new ValidacionException("El tamaño de pagina debe estar entre 1 y " + TAMANO_MAXIMO_PAGINA);
        }
    }

    private void validarId(Long idAdministrador) {
        if (idAdministrador == null || idAdministrador <= 0) {
            throw new ValidacionException("El id del administrador debe ser positivo");
        }
    }

    private void registrarInvalidacionTrasConfirmar(Long idAdministrador) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sesiones.revocarTodas(idAdministrador);
                recuperaciones.invalidarUsuario(idAdministrador);
                cambiosCorreo.invalidarUsuario(idAdministrador);
            }
        });
    }

    private RecursoNoEncontradoException administradorNoEncontrado() {
        return new RecursoNoEncontradoException("El administrador no existe")
                .conCodigo("ADMINISTRADOR_NO_ENCONTRADO");
    }

    private ConflictoException correoYaRegistrado() {
        return new ConflictoException("El correo ya se encuentra registrado")
                .conCodigo(CodigosError.CORREO_YA_REGISTRADO);
    }

    private static String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }
}
