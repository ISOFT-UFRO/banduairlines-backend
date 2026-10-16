package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.ActualizacionFidelizacion;
import com.banduairlines.dto.cuenta.RespuestaCanje;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.model.Beneficio;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.MotivoBloqueo;
import com.banduairlines.persistence.cuenta.RegistroPuntos;
import com.banduairlines.persistence.cuenta.TipoRegistroPuntos;
import com.banduairlines.repository.BeneficioRepository;
import com.banduairlines.repository.CuentaPuntosRepository;
import com.banduairlines.repository.HistorialPuntosRepository;
import com.banduairlines.repository.RegistroPuntosRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.cuenta.ActualizacionFidelizacionService;
import com.banduairlines.service.cuenta.CanjeService;
import com.banduairlines.service.cuenta.CodigosFidelizacion;
import com.banduairlines.service.cuenta.EvaluadorBeneficio;
import com.banduairlines.service.cuenta.ReferenciaCanje;
import com.banduairlines.service.cuenta.RegistroPuntosService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
public class CanjeServiceImpl implements CanjeService {

    private final CuentaPuntosRepository cuentas;
    private final BeneficioRepository beneficios;
    private final HistorialPuntosRepository historial;
    private final RegistroPuntosRepository registros;
    private final ActualizacionFidelizacionService actualizacion;
    private final EvaluadorBeneficio evaluador;
    private final UsuarioActual usuarioActual;
    private final Clock reloj;

    public CanjeServiceImpl(CuentaPuntosRepository cuentas, BeneficioRepository beneficios,
                            HistorialPuntosRepository historial, RegistroPuntosRepository registros,
                            ActualizacionFidelizacionService actualizacion, EvaluadorBeneficio evaluador,
                            UsuarioActual usuarioActual, Clock reloj) {
        this.cuentas = cuentas;
        this.beneficios = beneficios;
        this.historial = historial;
        this.registros = registros;
        this.actualizacion = actualizacion;
        this.evaluador = evaluador;
        this.usuarioActual = usuarioActual;
        this.reloj = reloj;
    }

    /**
     * Registra un movimiento CANJE. No modifica millas ni baja el nivel.
     * <p>
     * La cuenta queda bloqueada (PESSIMISTIC_WRITE) hasta el fin de la transacción, así dos canjes
     * simultáneos del mismo pasajero se ejecutan uno después del otro y el saldo nunca queda negativo.
     * Las acumulaciones de puntos toman el mismo bloqueo.
     */
    @Override
    @Transactional
    public ResultadoCanje canjearActual(Long idBeneficio, String claveIdempotencia) {
        String clave = ReferenciaCanje.validarClave(claveIdempotencia);
        if (idBeneficio == null || idBeneficio <= 0) {
            throw new ValidacionException("El idBeneficio debe ser un número positivo");
        }
        Long idUsuario = usuarioActual.exigirId();
        Usuario usuario = cuentas.bloquear(idUsuario).orElseThrow(RegistroPuntosService::cuentaNoEncontrada);
        RegistroPuntosService.exigirPasajero(usuario);

        Optional<RegistroPuntos> anterior = historial.findFirstByIdUsuarioAndTipoAndReferenciaStartingWith(
                idUsuario, TipoRegistroPuntos.CANJE, ReferenciaCanje.prefijo(clave));
        if (anterior.isPresent()) {
            return canjeRepetido(anterior.get(), idBeneficio, idUsuario);
        }

        Beneficio beneficio = beneficios.findById(idBeneficio).orElseThrow(() ->
                new RecursoNoEncontradoException("Beneficio", idBeneficio)
                        .conCodigo(CodigosFidelizacion.BENEFICIO_NO_ENCONTRADO));
        if (beneficio.getCostoPuntos() == null || beneficio.getCostoPuntos() <= 0) {
            throw new ReglaNegocioException("El beneficio no tiene un costo en puntos válido");
        }

        ActualizacionFidelizacion estado = actualizacion.actualizarActual();
        Optional<MotivoBloqueo> motivo = evaluador.evaluar(beneficio, estado.nivelActual(), estado.saldoPuntos());
        if (motivo.isPresent()) {
            throw new ReglaNegocioException(mensaje(motivo.get())).conCodigo(motivo.get().name());
        }

        RegistroPuntos canje = registros.save(new RegistroPuntos(
                idUsuario,
                TipoRegistroPuntos.CANJE,
                beneficio.getCostoPuntos(),
                reloj.instant(),
                ReferenciaCanje.crear(clave, idBeneficio),
                null
        ));
        long saldoFinal = estado.saldoPuntos() - beneficio.getCostoPuntos();
        return new ResultadoCanje(new RespuestaCanje(
                canje.getIdRegistro(),
                beneficio.getIdBeneficio(),
                beneficio.getNombre(),
                canje.getCantidad(),
                saldoFinal,
                canje.getFecha()
        ), true);
    }

    /**
     * La misma clave con el mismo beneficio devuelve el canje original sin registrar otro.
     * La misma clave con otro beneficio es un conflicto.
     */
    private ResultadoCanje canjeRepetido(RegistroPuntos anterior, Long idBeneficio, Long idUsuario) {
        Long idBeneficioAnterior = ReferenciaCanje.idBeneficio(anterior.getReferencia()).orElse(null);
        if (!idBeneficio.equals(idBeneficioAnterior)) {
            throw new ConflictoException("La Clave-Idempotencia ya se usó para canjear otro beneficio")
                    .conCodigo(CodigosFidelizacion.CANJE_DUPLICADO);
        }
        String nombre = beneficios.findById(idBeneficio).map(Beneficio::getNombre).orElse(null);
        return new ResultadoCanje(new RespuestaCanje(
                anterior.getIdRegistro(),
                idBeneficio,
                nombre,
                anterior.getCantidad(),
                registros.calcularSaldo(idUsuario),
                anterior.getFecha()
        ), false);
    }

    private static String mensaje(MotivoBloqueo motivo) {
        return switch (motivo) {
            case BENEFICIO_AGOTADO -> "El beneficio no está disponible";
            case NIVEL_INSUFICIENTE -> "Tu nivel no alcanza para este beneficio";
            case PUNTOS_INSUFICIENTES -> "No tienes puntos suficientes para este beneficio";
        };
    }
}
