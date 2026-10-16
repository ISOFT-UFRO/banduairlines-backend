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
                .map(registro -> movimiento(registro, nombres))
                .toList();
    }

    private static RespuestaMovimientoPuntos movimiento(RegistroPuntos registro, Map<Long, String> nombres) {
        Long idBeneficio = ReferenciaCanje.idBeneficio(registro.getReferencia()).orElse(null);
        return new RespuestaMovimientoPuntos(
                registro.getIdRegistro(),
                registro.getTipo(),
                registro.getCantidad(),
                registro.getFecha(),
                descripcion(registro, idBeneficio, nombres),
                registro.getCodigoPNR(),
                idBeneficio
        );
    }

    private static String descripcion(RegistroPuntos registro, Long idBeneficio, Map<Long, String> nombres) {
        if (registro.getTipo() == TipoRegistroPuntos.CANJE) {
            String nombre = idBeneficio == null ? null : nombres.get(idBeneficio);
            return nombre == null ? "Canje de beneficio" : "Canje de " + nombre;
        }
        String referencia = registro.getReferencia();
        if (referencia != null && referencia.startsWith(PREFIJO_VUELO)) {
            return "Acumulación por vuelo " + referencia.substring(PREFIJO_VUELO.length());
        }
        return "Acumulación de puntos";
    }
}
