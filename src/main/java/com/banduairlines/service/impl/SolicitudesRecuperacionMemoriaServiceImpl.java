package com.banduairlines.service.impl;

import com.banduairlines.service.cuenta.SolicitudesRecuperacionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@Service
public class SolicitudesRecuperacionMemoriaServiceImpl
        implements SolicitudesRecuperacionService {

    private final GeneradorTokenRecuperacion generador;
    private final Duration duracion;
    private final java.time.Clock reloj;

    private final Map<String, SolicitudTemporal> solicitudes =
            new HashMap<>();

    private final Map<Long, String> ultimaPorUsuario =
            new HashMap<>();

    public SolicitudesRecuperacionMemoriaServiceImpl(
            GeneradorTokenRecuperacion generador,
            @Value("${app.cuenta.recuperacion.expiracion-minutos:10}")
            long minutosExpiracion, java.time.Clock reloj) {

        if (minutosExpiracion <= 0) {
            throw new IllegalArgumentException(
                    "La expiracion debe ser positiva"
            );
        }

        this.generador = generador;
        this.reloj = reloj;
        this.duracion = Duration.ofMinutes(minutosExpiracion);
    }

    @Override
    public synchronized TokenEmitido crearSolicitud(
            Long idUsuario) {

        if (idUsuario == null || idUsuario <= 0) {
            throw new IllegalArgumentException(
                    "Identificador de usuario invalido"
            );
        }

        Instant ahora = reloj.instant();
        limpiarExpiradas(ahora);

        // Crear token nuevo y evitar colisiones.
        String token;
        String tokenHash;

        do {
            token = generador.generarToken();
            tokenHash = generador.calcularHash(token);

        } while (solicitudes.containsKey(tokenHash));

        Instant fechaExpiracion = ahora.plus(duracion);

        // Invalidar cualquier solicitud previa del usuario.
        String hashAnterior =
                ultimaPorUsuario.put(idUsuario, tokenHash);

        if (hashAnterior != null) {
            solicitudes.remove(hashAnterior);
        }

        SolicitudTemporal solicitud = new SolicitudTemporal(
                idUsuario,
                tokenHash,
                ahora,
                fechaExpiracion
        );

        solicitudes.put(tokenHash, solicitud);

        return new TokenEmitido(token, fechaExpiracion);
    }

    @Override
    public synchronized boolean esValido(String token) {

        Instant ahora = reloj.instant();
        limpiarExpiradas(ahora);

        String hash = calcularHashSeguro(token);

        if (hash == null) {
            return false;
        }

        SolicitudTemporal solicitud = solicitudes.get(hash);

        return estaDisponible(solicitud, hash, ahora);
    }

    @Override
    public synchronized Optional<Long> consumirSolicitud(String token, java.util.function.LongConsumer operacion) {

        Instant ahora = reloj.instant();
        limpiarExpiradas(ahora);

        String hash = calcularHashSeguro(token);

        if (hash == null) {
            return Optional.empty();
        }

        SolicitudTemporal solicitud = solicitudes.get(hash);

        if (!estaDisponible(solicitud, hash, ahora)) {
            return Optional.empty();
        }

        operacion.accept(solicitud.idUsuario);
        solicitud.fechaUso = reloj.instant();

        ultimaPorUsuario.remove(solicitud.idUsuario, hash);

        return Optional.of(solicitud.idUsuario);
    }

    @Override
    public synchronized EstadoToken estadoToken(String token) {

        Instant ahora = reloj.instant();

        limpiarExpiradas(ahora);

        String hash = calcularHashSeguro(token);

        if (hash == null) {return EstadoToken.INVALIDO;}

        SolicitudTemporal solicitud = solicitudes.get(hash);

        if (solicitud == null) {return EstadoToken.INVALIDO;}

        if (solicitud.fechaUso != null) {return EstadoToken.UTILIZADO;}

        if (!solicitud.fechaExpiracion.isAfter(ahora)) {return EstadoToken.VENCIDO;}

        if (!hash.equals(
                ultimaPorUsuario.get(solicitud.idUsuario))) {
            return EstadoToken.INVALIDO;
        }
        return EstadoToken.VALIDO;
    }


    @Override
    public synchronized void invalidarUsuario(Long idUsuario) {
        solicitudes.values().removeIf(s -> s.idUsuario.equals(idUsuario));
        ultimaPorUsuario.remove(idUsuario);
    }

    private boolean estaDisponible(
            SolicitudTemporal solicitud,
            String hash,
            Instant ahora) {

        if (solicitud == null) {
            return false;
        }

        boolean noUtilizada = solicitud.fechaUso == null;

        boolean noVencida =
                solicitud.fechaExpiracion.isAfter(ahora);

        boolean esLaUltima = hash.equals(
                ultimaPorUsuario.get(solicitud.idUsuario)
        );

        return noUtilizada && noVencida && esLaUltima;
    }

    private String calcularHashSeguro(String token) {

        try {
            return generador.calcularHash(token);

        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void limpiarExpiradas(Instant ahora) {

        Iterator<Map.Entry<String, SolicitudTemporal>> iterator =
                solicitudes.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<String, SolicitudTemporal> entrada =
                    iterator.next();

            SolicitudTemporal solicitud = entrada.getValue();

            if (!solicitud.fechaExpiracion
                    .plus(Duration.ofMinutes(60))
                    .isAfter(ahora)) {

                ultimaPorUsuario.remove(
                        solicitud.idUsuario,
                        entrada.getKey()
                );

                iterator.remove();
            }
        }
    }

    private static final class SolicitudTemporal {

        private final Long idUsuario;
        private final String tokenHash;
        private final Instant fechaCreacion;
        private final Instant fechaExpiracion;

        private Instant fechaUso;

        private SolicitudTemporal(
                Long idUsuario,
                String tokenHash,
                Instant fechaCreacion,
                Instant fechaExpiracion) {

            this.idUsuario = idUsuario;
            this.tokenHash = tokenHash;
            this.fechaCreacion = fechaCreacion;
            this.fechaExpiracion = fechaExpiracion;
        }
    }
}
