package com.banduairlines.service.cuenta;

import com.banduairlines.exception.*;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.persistence.cuenta.*;
import com.banduairlines.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

@Service
public class RegistroPuntosService {
    private final RegistroPuntosRepository registros;
    private final CuentaPuntosRepository cuentas;
    private final Clock reloj;

    public RegistroPuntosService(RegistroPuntosRepository registros, CuentaPuntosRepository cuentas, Clock reloj) {
        this.registros = registros;
        this.cuentas = cuentas;
        this.reloj = reloj;
    }

    @Transactional
    public void acumular(Long idUsuario, long cantidad, String referencia, String codigoPNR) {
        if (codigoPNR == null || !codigoPNR.matches("[A-Z0-9]{6}"))
            throw new ValidacionException("El codigo PNR debe contener seis letras mayusculas o numeros");
        registrar(idUsuario, cantidad, referencia, codigoPNR);
    }

    @Transactional(readOnly = true)
    public void verificarHistorialCompleto(Long idUsuario, java.util.Set<String> referencias) {
        boolean incompleto = registros.findByIdUsuarioAndTipo(idUsuario, TipoRegistroPuntos.ACUMULACION).stream()
                .map(RegistroPuntos::getReferencia).filter(r -> r.startsWith("VUELO:"))
                .anyMatch(r -> !referencias.contains(r));
        if (incompleto) throw new ServicioExternoException("El historial omite vuelos ya acreditados")
                .conCodigo("MILLAS_NO_DISPONIBLES");
    }

    @Transactional(readOnly = true)
    public long consultarSaldo(Long idUsuario) {
        exigirPasajero(cuentas.findById(idUsuario).orElseThrow(RegistroPuntosService::cuentaNoEncontrada));
        return registros.calcularSaldo(idUsuario);
    }

    private void registrar(Long idUsuario, long cantidad, String referencia, String pnr) {
        if (cantidad <= 0 || referencia == null || referencia.isBlank() || referencia.length() > 120)
            throw new ValidacionException("Cantidad y referencia invalidas");
        exigirPasajero(cuentas.bloquear(idUsuario).orElseThrow(RegistroPuntosService::cuentaNoEncontrada));
        var anterior = registros.findByIdUsuarioAndTipoAndReferencia(idUsuario, TipoRegistroPuntos.ACUMULACION, referencia);
        if (anterior.isPresent()) {
            var registro = anterior.get();
            if (registro.getCantidad() != cantidad || !java.util.Objects.equals(pnr, registro.getCodigoPNR()))
                throw new ConflictoException("La referencia ya fue utilizada con otros datos").conCodigo("REFERENCIA_PUNTOS_CONFLICTIVA");
            return;
        }
        long saldo = registros.calcularSaldo(idUsuario);
        try { Math.addExact(saldo, cantidad); }
        catch (ArithmeticException e) { throw new ValidacionException("Saldo fuera del rango permitido"); }
        registros.save(new RegistroPuntos(idUsuario, TipoRegistroPuntos.ACUMULACION, cantidad, reloj.instant(), referencia, pnr));
    }

    public static void exigirPasajero(Usuario usuario) {
        if (!Boolean.TRUE.equals(usuario.getActivo()) || usuario.getRol() != RolUsuario.PASAJERO)
            throw new AccessDeniedException("Solo pasajeros activos participan en fidelizacion");
    }

    public static RecursoNoEncontradoException cuentaNoEncontrada() {
        return new RecursoNoEncontradoException("La cuenta no existe").conCodigo("CUENTA_NO_ENCONTRADA");
    }
}
