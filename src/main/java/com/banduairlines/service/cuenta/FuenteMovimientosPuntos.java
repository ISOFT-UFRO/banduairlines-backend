package com.banduairlines.service.cuenta;

import com.banduairlines.model.enums.TipoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Movimientos de puntos que consume la fidelización: historial, canjes registrados y registro de un canje.
 * <p>
 * La implementación pertenece al módulo que guarda los movimientos de puntos. La fidelización no accede a ese
 * almacenamiento: solo valida, calcula y transforma lo que recibe por esta interfaz.
 * <p>
 * Toda implementación debe lanzar {@code ServicioExternoException} con el código
 * {@code MOVIMIENTOS_NO_DISPONIBLES} cuando no pueda consultar o registrar los movimientos.
 */
public interface FuenteMovimientosPuntos {

    /** Historial de acumulaciones y canjes del usuario, del más reciente al más antiguo. */
    Page<MovimientoPuntos> consultarMovimientos(Long idUsuario, Pageable pagina);

    /**
     * Canjes registrados de cada beneficio, considerando a todos los usuarios. Un reintento con la misma clave no
     * cuenta dos veces. Los beneficios sin canjes pueden omitirse del resultado.
     */
    Map<Long, Long> contarCanjes(Collection<Long> idsBeneficio);

    /** Canje que el usuario registró con esa clave de idempotencia, si existe. */
    Optional<CanjeRegistrado> buscarCanje(Long idUsuario, String claveIdempotencia);

    /**
     * Registra un canje de forma atómica y persistente: un único movimiento CANJE de {@code costoPuntos} que deja el
     * saldo en {@code saldoPosterior}, sin modificar millas ni nivel.
     * <ul>
     *     <li>Si el usuario ya registró la clave, no descuenta otra vez y devuelve el canje original con
     *     {@code creado = false}, aunque se trate de otro beneficio.</li>
     *     <li>Debe volver a comprobar, dentro de la misma operación, el saldo y que los canjes del beneficio sigan
     *     bajo {@code stockInicial}; si no, lanza {@code ReglaNegocioException} con {@code PUNTOS_INSUFICIENTES} o
     *     {@code BENEFICIO_AGOTADO}.</li>
     *     <li>Debe conservar el nombre del beneficio y los saldos, para que el canje se pueda devolver igual
     *     después aunque cambie el catálogo.</li>
     * </ul>
     */
    ResultadoRegistroCanje registrarCanje(SolicitudRegistroCanje solicitud);

    /**
     * Movimiento de puntos. La cantidad siempre es positiva; el tipo indica si suma (ACUMULACION) o resta (CANJE).
     *
     * @param codigoPNR   reserva asociada a una acumulación; null en los canjes
     * @param idBeneficio beneficio canjeado; null en las acumulaciones
     */
    record MovimientoPuntos(Long idMovimiento, TipoMovimiento tipo, long cantidad, Instant fecha,
                            String descripcion, String codigoPNR, Long idBeneficio) {
    }

    /** Datos que la fidelización entrega para registrar un canje ya validado. */
    record SolicitudRegistroCanje(Long idUsuario, String claveIdempotencia, Long idBeneficio,
                                  String nombreBeneficio, long costoPuntos, long stockInicial,
                                  long saldoAnterior, long saldoPosterior) {
    }

    /** Canje registrado, con el nombre del beneficio y los saldos de ese momento. */
    record CanjeRegistrado(MovimientoPuntos movimiento, String nombreBeneficio,
                           long saldoAnterior, long saldoPosterior) {
    }

    /** @param creado true si el canje se registró en esta llamada; false si la clave ya estaba registrada */
    record ResultadoRegistroCanje(CanjeRegistrado canje, boolean creado) {
    }
}
