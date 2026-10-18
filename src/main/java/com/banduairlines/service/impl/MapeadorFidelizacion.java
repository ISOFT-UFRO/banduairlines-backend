package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.RespuestaBeneficio;
import com.banduairlines.dto.cuenta.RespuestaCanje;
import com.banduairlines.dto.cuenta.RespuestaMovimientoPuntos;
import com.banduairlines.dto.cuenta.MotivoBloqueo;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.model.enums.TipoMovimiento;
import com.banduairlines.service.cuenta.BeneficioConfigurado;
import com.banduairlines.service.cuenta.EvaluadorBeneficio;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos.CanjeRegistrado;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos.MovimientoPuntos;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MapeadorFidelizacion {

    private final EvaluadorBeneficio evaluador;

    public MapeadorFidelizacion(EvaluadorBeneficio evaluador) {
        this.evaluador = evaluador;
    }

    public RespuestaBeneficio beneficio(BeneficioConfigurado beneficio, Nivel nivelUsuario,
                                        long saldoPuntos, long stockRestante) {
        MotivoBloqueo motivo = evaluador.evaluar(beneficio, nivelUsuario, saldoPuntos, stockRestante).orElse(null);
        return new RespuestaBeneficio(
                beneficio.id(),
                beneficio.nombre(),
                beneficio.descripcion(),
                beneficio.nivelRequerido(),
                beneficio.costoPuntos(),
                motivo == null,
                motivo
        );
    }

    /**
     * Evalúa cada beneficio con el stock restante: stock inicial menos los canjes registrados.
     *
     * @param canjesPorBeneficio canjes registrados por id de beneficio; los ausentes cuentan como cero
     */
    public List<RespuestaBeneficio> beneficios(List<BeneficioConfigurado> beneficios, Nivel nivelUsuario,
                                               long saldoPuntos, Map<Long, Long> canjesPorBeneficio) {
        return beneficios.stream()
                .map(beneficio -> beneficio(beneficio, nivelUsuario, saldoPuntos,
                        beneficio.stockInicial() - canjesPorBeneficio.getOrDefault(beneficio.id(), 0L)))
                .toList();
    }

    public List<RespuestaMovimientoPuntos> movimientos(List<MovimientoPuntos> movimientos) {
        return movimientos.stream().map(this::movimiento).toList();
    }

    /**
     * La cantidad llega siempre positiva y el tipo indica si suma o resta.
     * En la API, cantidadPuntos lleva el signo: positivo al acumular y negativo al canjear.
     */
    public RespuestaMovimientoPuntos movimiento(MovimientoPuntos movimiento) {
        long cantidadPuntos = movimiento.tipo() == TipoMovimiento.CANJE
                ? -movimiento.cantidad()
                : movimiento.cantidad();
        return new RespuestaMovimientoPuntos(
                movimiento.idMovimiento(),
                movimiento.tipo(),
                cantidadPuntos,
                movimiento.fecha(),
                movimiento.descripcion(),
                movimiento.codigoPNR(),
                movimiento.idBeneficio()
        );
    }

    public RespuestaCanje canje(CanjeRegistrado canje) {
        return new RespuestaCanje(
                canje.movimiento().idMovimiento(),
                canje.saldoAnterior(),
                canje.movimiento().cantidad(),
                canje.saldoPosterior(),
                movimiento(canje.movimiento())
        );
    }
}
