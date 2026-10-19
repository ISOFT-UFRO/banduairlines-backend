package com.banduairlines.service.reserva;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.banduairlines.exception.DemasiadosIntentosException;

//endpoint 14 de intentos de bsuqueda
@Component
public class LimitadorIntentosBusqueda {

    static final int MAX_INTENTOS_POR_IP = 15;
    static final int MAX_FALLOS_POR_CLAVE = 5;
    static final Duration VENTANA = Duration.ofMinutes(10);
    private static final int CLAVES_ANTES_DE_LIMPIAR = 10_000;

    private final Map<String, Deque<Instant>> registros = new ConcurrentHashMap<>();

    // Lanza DemasiadosIntentosException (429) si alguna de las tres claves ya agotó su cupo
    public void exigirCupo(String ip, String codigo, String correo) {
        if (excedido("ip:" + ip, MAX_INTENTOS_POR_IP)
                || excedido("codigo:" + codigo, MAX_FALLOS_POR_CLAVE)
                || excedido("correo:" + correo, MAX_FALLOS_POR_CLAVE)) {
            throw new DemasiadosIntentosException(VENTANA.toSeconds());
        }
    }

    public void registrarIntento(String ip) {
        registrar("ip:" + ip);
    }

    public void registrarFallo(String codigo, String correo) {
        registrar("codigo:" + codigo);
        registrar("correo:" + correo);
    }

    private boolean excedido(String clave, int maximo) {
        Deque<Instant> registro = registros.get(clave);
        if (registro == null) {
            return false;
        }
        synchronized (registro) {
            purgar(registro);
            return registro.size() >= maximo;
        }
    }

    private void registrar(String clave) {
        if (registros.size() > CLAVES_ANTES_DE_LIMPIAR) {
            limpiar();
        }
        Deque<Instant> registro = registros.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (registro) {
            purgar(registro);
            registro.addLast(Instant.now());
        }
    }

    private void limpiar() {
        registros.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                purgar(e.getValue());
                return e.getValue().isEmpty();
            }
        });
    }

    private static void purgar(Deque<Instant> registro) {
        Instant limite = Instant.now().minus(VENTANA);
        while (!registro.isEmpty() && registro.peekFirst().isBefore(limite)) {
            registro.pollFirst();
        }
    }
}