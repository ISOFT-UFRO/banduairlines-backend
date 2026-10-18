package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.ConfirmacionCambioCorreo;
import com.banduairlines.dto.cuenta.RespuestaSolicitudCambioCorreo;
import com.banduairlines.dto.cuenta.RespuestaUsuario;
import com.banduairlines.exception.CodigosError;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.RecursoVencidoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.exception.ServicioExternoException;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.cuenta.CambioCorreoService;
import com.banduairlines.service.cuenta.ServicioCorreo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CambioCorreoServiceImpl implements CambioCorreoService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioActual usuarioActual;
    private final PasswordEncoder passwordEncoder;
    private final Optional<ServicioCorreo> servicioCorreo;
    private final Clock clock;
    private final long minutosExpiracion;
    private final SecureRandom secureRandom = new SecureRandom();
    private final AtomicLong secuenciaSolicitudes = new AtomicLong();
    private final ConcurrentMap<Long, SolicitudPendiente> solicitudes =
            new ConcurrentHashMap<>();
    private final Object monitorSolicitudes = new Object();

    public CambioCorreoServiceImpl(
            UsuarioRepository usuarioRepository,
            UsuarioActual usuarioActual,
            PasswordEncoder passwordEncoder,
            Optional<ServicioCorreo> servicioCorreo,
            Clock clock,
            @Value("${app.cuenta.cambio-correo.expiracion-minutos:15}")
            long minutosExpiracion
    ) {
        if (minutosExpiracion <= 0) {
            throw new IllegalArgumentException("La expiracion del codigo debe ser positiva");
        }
        this.usuarioRepository = usuarioRepository;
        this.usuarioActual = usuarioActual;
        this.passwordEncoder = passwordEncoder;
        this.servicioCorreo = servicioCorreo;
        this.clock = clock;
        this.minutosExpiracion = minutosExpiracion;
    }

    @Override
    @Transactional
    public RespuestaSolicitudCambioCorreo solicitar(
            com.banduairlines.dto.cuenta.SolicitudCambioCorreo solicitud
    ) {
        Usuario usuario = obtenerUsuarioActual();
        String nuevoCorreo = normalizarCorreo(solicitud.nuevoCorreo());

        if (nuevoCorreo.equals(usuario.getEmail())) {
            throw new ValidacionException("El nuevo correo debe ser diferente del correo actual");
        }
        validarCorreoDisponible(nuevoCorreo, usuario.getId());

        Instant ahora = clock.instant();
        String codigo = generarCodigo();
        Instant expiracion = ahora.plus(minutosExpiracion, ChronoUnit.MINUTES);
        enviarCodigo(nuevoCorreo, codigo, expiracion);

        SolicitudPendiente guardada;
        synchronized (monitorSolicitudes) {
            invalidarSolicitudesAnteriores(usuario.getId(), ahora);
            Long idSolicitud = secuenciaSolicitudes.incrementAndGet();
            guardada = new SolicitudPendiente(
                    idSolicitud,
                    usuario.getId(),
                    nuevoCorreo,
                    passwordEncoder.encode(codigo),
                    expiracion
            );
            solicitudes.put(idSolicitud, guardada);
        }

        return new RespuestaSolicitudCambioCorreo(
                guardada.idSolicitud,
                enmascararCorreo(nuevoCorreo),
                expiracion
        );
    }

    @Override
    @Transactional
    public RespuestaUsuario confirmar(
            Long idSolicitud,
            ConfirmacionCambioCorreo confirmacion
    ) {
        Usuario usuario = obtenerUsuarioActual();
        SolicitudPendiente solicitud = buscarSolicitud(idSolicitud, usuario.getId());
        Instant ahora = clock.instant();

        synchronized (solicitud) {
            if (!solicitud.puedeUtilizarse()) {
                throw codigoIncorrecto("La solicitud ya fue finalizada");
            }
            if (solicitud.estaVencida(ahora)) {
                throw new RecursoVencidoException(
                        CodigosError.CODIGO_VENCIDO,
                        "El codigo de verificacion ha vencido"
                );
            }
            if (!passwordEncoder.matches(confirmacion.codigo(), solicitud.codigoHash)) {
                throw codigoIncorrecto("El codigo de verificacion es incorrecto");
            }

            validarCorreoDisponible(solicitud.nuevoCorreo, usuario.getId());
            usuario.setEmail(solicitud.nuevoCorreo);

            try {
                usuarioRepository.saveAndFlush(usuario);
            } catch (DataIntegrityViolationException ex) {
                throw correoYaRegistrado();
            }

            confirmarAlCompletarTransaccion(solicitud, ahora);
        }

        return MapeadorUsuario.aRespuesta(usuario);
    }

    @Override
    @Transactional
    public void cancelar(Long idSolicitud) {
        Usuario usuario = obtenerUsuarioActual();
        SolicitudPendiente solicitud = buscarSolicitud(idSolicitud, usuario.getId());

        synchronized (solicitud) {
            if (solicitud.fueCancelada() || solicitud.fueConfirmada()) {
                return;
            }

            solicitud.fechaCancelacion = clock.instant();
            solicitud.codigoHash = null;
        }
    }

    private Usuario obtenerUsuarioActual() {
        Long idUsuario = usuarioActual.exigirId();
        Usuario usuario = usuarioRepository.buscarParaActualizar(idUsuario)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "La cuenta autenticada ya no esta disponible"
                ));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new AuthenticationCredentialsNotFoundException("La cuenta no esta activa");
        }
        if (usuario.getRol() != RolUsuario.PASAJERO
                && usuario.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new AccessDeniedException("El rol no puede cambiar el correo");
        }
        return usuario;
    }

    @Override
    public void invalidarUsuario(Long idUsuario) {
        synchronized (monitorSolicitudes) {
            invalidarSolicitudesAnteriores(idUsuario, clock.instant());
            solicitudes.entrySet().removeIf(e -> e.getValue().idUsuario.equals(idUsuario));
        }
    }

    private SolicitudPendiente buscarSolicitud(
            Long idSolicitud,
            Long idUsuario
    ) {
        SolicitudPendiente solicitud = solicitudes.get(idSolicitud);
        if (solicitud == null || !solicitud.idUsuario.equals(idUsuario)) {
            throw new RecursoNoEncontradoException(
                    "La solicitud de cambio de correo no existe"
            ).conCodigo(CodigosError.SOLICITUD_NO_ENCONTRADA);
        }
        return solicitud;
    }

    private void invalidarSolicitudesAnteriores(Long idUsuario, Instant ahora) {
        solicitudes.values().forEach(anterior -> {
            synchronized (anterior) {
                if (anterior.idUsuario.equals(idUsuario) && anterior.puedeUtilizarse()) {
                    anterior.fechaCancelacion = ahora;
                    anterior.codigoHash = null;
                }
            }
        });
    }

    private void validarCorreoDisponible(String correo, Long idUsuarioActual) {
        usuarioRepository.findByEmail(correo).ifPresent(usuario -> {
            if (!usuario.getId().equals(idUsuarioActual)) {
                throw correoYaRegistrado();
            }
        });
    }

    private void enviarCodigo(String correo, String codigo, Instant expiracion) {
        ServicioCorreo correoCompartido = servicioCorreo.orElseThrow(() ->
                new ServicioExternoException(
                        "El servicio compartido de correo aun no esta disponible"
                ));
        try {
            correoCompartido.enviarCodigoCambioCorreo(correo, codigo, expiracion);
        } catch (ServicioExternoException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ServicioExternoException(
                    "No fue posible enviar el codigo de cambio de correo",
                    ex
            );
        }
    }

    private String generarCodigo() {
        return String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private String enmascararCorreo(String correo) {
        int separador = correo.indexOf('@');
        return correo.substring(0, 1) + "*****" + correo.substring(separador);
    }

    private void confirmarAlCompletarTransaccion(SolicitudPendiente solicitud, Instant ahora) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            solicitud.fechaConfirmacion = ahora;
            solicitud.codigoHash = null;
            return;
        }

        solicitud.confirmacionEnCurso = true;
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    synchronized (solicitud) {
                        if (status == STATUS_COMMITTED) {
                            solicitud.fechaConfirmacion = ahora;
                            solicitud.codigoHash = null;
                        }
                        solicitud.confirmacionEnCurso = false;
                    }
                }
            });
        } catch (RuntimeException ex) {
            solicitud.confirmacionEnCurso = false;
            throw ex;
        }
    }

    private ConflictoException correoYaRegistrado() {
        return new ConflictoException("El correo ya se encuentra registrado")
                .conCodigo(CodigosError.CORREO_YA_REGISTRADO);
    }

    private ReglaNegocioException codigoIncorrecto(String mensaje) {
        return new ReglaNegocioException(mensaje)
                .conCodigo(CodigosError.CODIGO_INCORRECTO);
    }

    private static final class SolicitudPendiente {

        private final Long idSolicitud;
        private final Long idUsuario;
        private final String nuevoCorreo;
        private String codigoHash;
        private final Instant fechaExpiracion;
        private Instant fechaConfirmacion;
        private Instant fechaCancelacion;
        private boolean confirmacionEnCurso;

        private SolicitudPendiente(
                Long idSolicitud,
                Long idUsuario,
                String nuevoCorreo,
                String codigoHash,
                Instant fechaExpiracion
        ) {
            this.idSolicitud = idSolicitud;
            this.idUsuario = idUsuario;
            this.nuevoCorreo = nuevoCorreo;
            this.codigoHash = codigoHash;
            this.fechaExpiracion = fechaExpiracion;
        }

        private boolean estaVencida(Instant ahora) {
            return !fechaExpiracion.isAfter(ahora);
        }

        private boolean fueConfirmada() {
            return fechaConfirmacion != null;
        }

        private boolean fueCancelada() {
            return fechaCancelacion != null;
        }

        private boolean puedeUtilizarse() {
            return !fueConfirmada() && !fueCancelada() && !confirmacionEnCurso && codigoHash != null;
        }
    }
}
