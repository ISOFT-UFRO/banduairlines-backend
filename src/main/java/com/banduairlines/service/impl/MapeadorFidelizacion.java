package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.RespuestaBeneficio;
import com.banduairlines.dto.cuenta.RespuestaMovimientoPuntos;
import com.banduairlines.model.Beneficio;
import com.banduairlines.model.enums.MotivoBloqueo;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.persistence.cuenta.RegistroPuntos;
import com.banduairlines.persistence.cuenta.TipoRegistroPuntos;
import com.banduairlines.repository.BeneficioRepository;
import com.banduairlines.service.cuenta.EvaluadorBeneficio;
import com.banduairlines.service.cuenta.ReferenciaCanje;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MapeadorFidelizacion {

    private static final String PREFIJO_VUELO = "VUELO:";

    private final BeneficioRepository beneficios;
    private final EvaluadorBeneficio evaluador;

    public MapeadorFidelizacion(BeneficioRepository beneficios, EvaluadorBeneficio evaluador) {
        this.beneficios = beneficios;
        this.evaluador = evaluador;
    }

    public RespuestaBeneficio beneficio(Beneficio beneficio, Nivel nivelUsuario, long saldoPuntos) {
        MotivoBloqueo motivo = evaluador.evaluar(beneficio, nivelUsuario, saldoPuntos).orElse(null);
        return new RespuestaBeneficio(
                beneficio.getIdBeneficio(),
                beneficio.getNombre(),
                beneficio.getDescripcion(),
                beneficio.getNivelRequerido(),
                beneficio.getCostoPuntos(),
                motivo == null,
                motivo
        );
    }

    public List<RespuestaMovimientoPuntos> movimientos(List<RegistroPuntos> registros) {
        Set<Long> idsBeneficio = registros.stream()
                .map(registro -> ReferenciaCanje.idBeneficio(registro.getReferencia()))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
        Map<Long, String> nombres = idsBeneficio.isEmpty()
                ? Map.of()
                : beneficios.findAllById(idsBeneficio).stream()
                        .collect(Collectors.toMap(Beneficio::getIdBeneficio, Beneficio::getNombre));
        return registros.stream()
                .map(registro -> {
                    Long idBeneficio = ReferenciaCanje.idBeneficio(registro.getReferencia()).orElse(null);
                    return movimiento(registro, idBeneficio == null ? null : nombres.get(idBeneficio));
                })
                .toList();
    }

    /** Movimiento de un canje recién registrado, cuando ya se conoce el nombre del beneficio. */
    public RespuestaMovimientoPuntos movimientoCanje(RegistroPuntos canje, String nombreBeneficio) {
        return movimiento(canje, nombreBeneficio);
    }

    /**
     * En la base la cantidad siempre es positiva y el tipo indica si suma o resta.
     * En la API, cantidadPuntos lleva el signo: positivo al acumular y negativo al canjear.
     */
    private static RespuestaMovimientoPuntos movimiento(RegistroPuntos registro, String nombreBeneficio) {
        Long idBeneficio = ReferenciaCanje.idBeneficio(registro.getReferencia()).orElse(null);
        long cantidadPuntos = registro.getTipo() == TipoRegistroPuntos.CANJE
                ? -registro.getCantidad()
                : registro.getCantidad();
        return new RespuestaMovimientoPuntos(
                registro.getIdRegistro(),
                registro.getTipo(),
                cantidadPuntos,
                registro.getFecha(),
                descripcion(registro, nombreBeneficio),
                registro.getCodigoPNR(),
                idBeneficio
        );
    }

    private static String descripcion(RegistroPuntos registro, String nombreBeneficio) {
        if (registro.getTipo() == TipoRegistroPuntos.CANJE) {
            return nombreBeneficio == null ? "Canje de beneficio" : "Canje de " + nombreBeneficio;
        }
        String referencia = registro.getReferencia();
        if (referencia != null && referencia.startsWith(PREFIJO_VUELO)) {
            return "Acumulación por vuelo " + referencia.substring(PREFIJO_VUELO.length());
        }
        return "Acumulación de puntos";
    }
}
