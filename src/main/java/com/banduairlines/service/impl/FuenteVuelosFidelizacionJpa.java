package com.banduairlines.service.impl;

import com.banduairlines.exception.ServicioExternoException;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.repository.*;
import com.banduairlines.service.cuenta.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class FuenteVuelosFidelizacionJpa implements FuenteVuelosFidelizacion {
    private final VuelosFidelizacionRepository vuelos;
    private final CuentaPuntosRepository cuentas;

    public FuenteVuelosFidelizacionJpa(VuelosFidelizacionRepository vuelos, CuentaPuntosRepository cuentas) {
        this.vuelos = vuelos;
        this.cuentas = cuentas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TramoRecorrido> consultar(Long idUsuario) {
        var usuario = cuentas.findById(idUsuario).orElseThrow(RegistroPuntosService::cuentaNoEncontrada);
        RegistroPuntosService.exigirPasajero(usuario);
        var reservas = vuelos.reservasConfirmadas(idUsuario);
        if (reservas.isEmpty()) return List.of();
        if (usuario.getTipoDocumento() == null || usuario.getNumeroDocumento() == null
                || usuario.getNumeroDocumento().isBlank()) {
            throw noDisponible("Falta identificar al titular por tipo y numero de documento");
        }
        Map<Long, TramoRecorrido> resultado = new LinkedHashMap<>();
        for (var reserva : reservas) {
            var boletos = vuelos.boletos(reserva.getId());
            if (boletos.isEmpty()) throw noDisponible("Reserva confirmada sin boletos disponibles");
            for (var boleto : boletos) {
                var pasajero = boleto.getPasajeroId();
                if (pasajero.getTipoDocumento() != usuario.getTipoDocumento()
                        || !normalizar(usuario.getNumeroDocumento()).equals(normalizar(pasajero.getNumeroDocumento()))) continue;
                var vuelo = boleto.getVueloId();
                var estados = vuelos.seguimiento(vuelo.getId(), PageRequest.of(0, 1));
                if (estados.isEmpty() || estados.getFirst().getEstado() == null)
                    throw noDisponible("Tracking no tiene estado para el vuelo " + vuelo.getId());
                var ultimo = estados.getFirst();
                if (ultimo.getEstado() != EstadoVuelo.ATERRIZADO) continue;
                var distancia = vuelo.getRuta().getDistanciaKm();
                if (distancia == null || distancia <= 0 || ultimo.getRegistradoEn() == null)
                    throw noDisponible("Falta distancia o fecha de llegada del vuelo " + vuelo.getId());
                resultado.putIfAbsent(vuelo.getId(), new TramoRecorrido(vuelo.getId(), reserva.getCodigoReserva(),
                        ultimo.getRegistradoEn(), BigDecimal.valueOf(distancia)));
            }
        }
        return List.copyOf(resultado.values());
    }

    private static String normalizar(String documento) {
        return documento == null ? "" : documento.trim().toUpperCase(Locale.ROOT);
    }

    private static ServicioExternoException noDisponible(String mensaje) {
        return new ServicioExternoException(mensaje).conCodigo("MILLAS_NO_DISPONIBLES");
    }
}
