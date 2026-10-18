package com.banduairlines.service.cuenta;

import com.banduairlines.exception.*;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.model.MovimientoMilla;
import com.banduairlines.model.enums.TipoMovimiento;
import com.banduairlines.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroPuntosService {
    private final MovimientoMillaRepository registros;
    private final CuentaPuntosRepository cuentas;
    private final ReservaRepository reservas;

    public RegistroPuntosService(MovimientoMillaRepository registros, CuentaPuntosRepository cuentas, ReservaRepository reservas) {
        this.registros = registros;
        this.cuentas = cuentas;
        this.reservas = reservas;
    }

    @Transactional
    public void acumular(Long idUsuario, long cantidad, String referencia, String codigoPNR) {
        if (codigoPNR == null || !codigoPNR.matches("[A-Z0-9]{6}"))
            throw new ValidacionException("El codigo PNR debe contener seis letras mayusculas o numeros");
        registrar(idUsuario, cantidad, referencia, codigoPNR);
    }

    @Transactional(readOnly = true)
    public void verificarHistorialCompleto(Long idUsuario, java.util.Set<String> referencias) {
        boolean incompleto = registros.findByCuentaFidelizacion_UsuarioIdAndTipo(idUsuario, TipoMovimiento.ACUMULACION).stream()
                .map(MovimientoMilla::getDescripcion).filter(r -> r != null && r.startsWith("VUELO:"))
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
        if (cantidad <= 0 || cantidad > Integer.MAX_VALUE || referencia == null || !referencia.matches("VUELO:[1-9][0-9]*") || referencia.length() > 200)
            throw new ValidacionException("Cantidad y referencia invalidas");
        var usuario = cuentas.bloquear(idUsuario).orElseThrow(RegistroPuntosService::cuentaNoEncontrada);
        exigirPasajero(usuario);
        if (usuario.getCuentaFidelizacion() == null) throw new ReglaNegocioException("Falta la cuenta de fidelizacion");
        var reserva = reservas.findByCodigoReservaAndUsuario_Id(pnr, idUsuario)
                .orElseThrow(() -> new ValidacionException("La reserva no pertenece a la cuenta"));
        var anterior = registros.findByCuentaFidelizacion_UsuarioIdAndTipoAndDescripcion(idUsuario, TipoMovimiento.ACUMULACION, referencia);
        if (anterior.isPresent()) {
            var registro = anterior.get();
            if (registro.getMillas().longValue() != cantidad || registro.getReserva() == null
                    || !reserva.getId().equals(registro.getReserva().getId()))
                throw new ConflictoException("La referencia ya fue utilizada con otros datos").conCodigo("REFERENCIA_PUNTOS_CONFLICTIVA");
            return;
        }
        long saldo = registros.calcularSaldo(idUsuario);
        try { Math.addExact(saldo, cantidad); }
        catch (ArithmeticException e) { throw new ValidacionException("Saldo fuera del rango permitido"); }
        registros.save(MovimientoMilla.builder()
                .cuentaFidelizacion(usuario.getCuentaFidelizacion())
                .reserva(reserva).tipo(TipoMovimiento.ACUMULACION)
                .millas((int) cantidad).descripcion(referencia).build());
    }

    public static void exigirPasajero(Usuario usuario) {
        if (!Boolean.TRUE.equals(usuario.getActivo()) || usuario.getRol() != RolUsuario.PASAJERO)
            throw new AccessDeniedException("Solo pasajeros activos participan en fidelizacion");
    }

    public static RecursoNoEncontradoException cuentaNoEncontrada() {
        return new RecursoNoEncontradoException("La cuenta no existe").conCodigo("CUENTA_NO_ENCONTRADA");
    }
}
