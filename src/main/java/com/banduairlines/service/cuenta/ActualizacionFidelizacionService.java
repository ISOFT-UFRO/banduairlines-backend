package com.banduairlines.service.cuenta;

import com.banduairlines.dto.cuenta.ActualizacionFidelizacion;
import com.banduairlines.exception.*;
import com.banduairlines.repository.CuentaPuntosRepository;
import com.banduairlines.security.UsuarioActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ActualizacionFidelizacionService {
    private static final BigDecimal KM_POR_MILLA = new BigDecimal("1.609344");
    private final FuenteVuelosFidelizacion fuente;
    private final GestionNivelService niveles;
    private final RegistroPuntosService puntos;
    private final CuentaPuntosRepository cuentas;
    private final UsuarioActual usuarioActual;

    public ActualizacionFidelizacionService(FuenteVuelosFidelizacion fuente, GestionNivelService niveles,
            RegistroPuntosService puntos, CuentaPuntosRepository cuentas, UsuarioActual usuarioActual) {
        this.fuente = fuente;
        this.niveles = niveles;
        this.puntos = puntos;
        this.cuentas = cuentas;
        this.usuarioActual = usuarioActual;
    }

    @Transactional
    public ActualizacionFidelizacion actualizarActual() {
        return actualizar(usuarioActual.exigirId());
    }

    private ActualizacionFidelizacion actualizar(Long idUsuario) {
        var usuario = cuentas.bloquear(idUsuario).orElseThrow(RegistroPuntosService::cuentaNoEncontrada);
        RegistroPuntosService.exigirPasajero(usuario);
        var cuenta = usuario.getCuentaFidelizacion();
        if (cuenta == null) throw new ReglaNegocioException("Falta la cuenta de fidelizacion");
        var tramos = consultarTramos(idUsuario);
        puntos.verificarHistorialCompleto(idUsuario, tramos.stream().map(t -> "VUELO:" + t.idVuelo())
                .collect(java.util.stream.Collectors.toSet()));
        BigDecimal kilometros = BigDecimal.ZERO;
        for (var tramo : tramos) {
            long millasPrevias = convertir(kilometros).setScale(0, RoundingMode.DOWN).longValueExact();
            var nivelPrevio = niveles.determinarNivel(millasPrevias);
            var multiplicador = niveles.obtenerConfiguracion(nivelPrevio).multiplicadorPuntos();
            long cantidad;
            try {
                cantidad = convertir(tramo.distanciaKm()).multiply(multiplicador)
                        .setScale(0, RoundingMode.DOWN).longValueExact();
            } catch (ArithmeticException ex) {
                throw new ValidacionException("Puntos fuera del rango permitido");
            }
            if (cantidad > 0) puntos.acumular(idUsuario, cantidad, "VUELO:" + tramo.idVuelo(), tramo.codigoPNR());
            kilometros = kilometros.add(tramo.distanciaKm());
        }
        BigDecimal millas = convertir(kilometros);
        var nivel = niveles.determinarNivel(millas.setScale(0, RoundingMode.DOWN).longValueExact());
        cuenta.setNivel(nivel);
        return new ActualizacionFidelizacion(millas.setScale(6, RoundingMode.DOWN), puntos.consultarSaldo(idUsuario),
                nivel, niveles.obtenerConfiguracion(nivel).multiplicadorPuntos());
    }

    private List<FuenteVuelosFidelizacion.TramoRecorrido> consultarTramos(Long idUsuario) {
        try {
            var respuesta = fuente.consultar(idUsuario);
            if (respuesta == null) throw new IllegalArgumentException("Historial ausente");
            Map<Long, FuenteVuelosFidelizacion.TramoRecorrido> unicos = new HashMap<>();
            for (var tramo : respuesta) {
                if (tramo == null || tramo.idVuelo() == null || tramo.idVuelo() <= 0 || tramo.fechaLlegada() == null
                        || tramo.codigoPNR() == null || !tramo.codigoPNR().matches("[A-Z0-9]{6}")
                        || tramo.distanciaKm() == null || tramo.distanciaKm().signum() <= 0)
                    throw new IllegalArgumentException("Tramo incompleto o invalido");
                var anterior = unicos.putIfAbsent(tramo.idVuelo(), tramo);
                if (anterior != null && !anterior.equals(tramo))
                    throw new IllegalArgumentException("Datos contradictorios para un vuelo");
            }
            return unicos.values().stream().sorted(Comparator
                    .comparing(FuenteVuelosFidelizacion.TramoRecorrido::fechaLlegada)
                    .thenComparing(FuenteVuelosFidelizacion.TramoRecorrido::idVuelo)).toList();
        } catch (Exception ex) {
            throw new ServicioExternoException("No fue posible obtener el historial completo de millas", ex)
                    .conCodigo("MILLAS_NO_DISPONIBLES");
        }
    }

    private static BigDecimal convertir(BigDecimal kilometros) {
        return kilometros.divide(KM_POR_MILLA, 12, RoundingMode.DOWN);
    }
}
