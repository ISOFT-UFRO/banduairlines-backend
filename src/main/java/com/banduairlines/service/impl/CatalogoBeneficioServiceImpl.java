package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.ActualizacionFidelizacion;
import com.banduairlines.dto.cuenta.RespuestaBeneficio;
import com.banduairlines.service.cuenta.ActualizacionFidelizacionService;
import com.banduairlines.service.cuenta.BeneficioConfigurado;
import com.banduairlines.service.cuenta.CatalogoBeneficioService;
import com.banduairlines.service.cuenta.CatalogoBeneficios;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CatalogoBeneficioServiceImpl implements CatalogoBeneficioService {

    private final ActualizacionFidelizacionService actualizacion;
    private final CatalogoBeneficios catalogo;
    private final AccesoMovimientosPuntos movimientos;
    private final MapeadorFidelizacion mapeador;

    public CatalogoBeneficioServiceImpl(ActualizacionFidelizacionService actualizacion, CatalogoBeneficios catalogo,
                                        AccesoMovimientosPuntos movimientos, MapeadorFidelizacion mapeador) {
        this.actualizacion = actualizacion;
        this.catalogo = catalogo;
        this.movimientos = movimientos;
        this.mapeador = mapeador;
    }

    /** Devuelve todo el catálogo, indicando para el pasajero si cada beneficio se puede canjear y, si no, por qué. */
    @Override
    @Transactional
    public List<RespuestaBeneficio> listarActual() {
        List<BeneficioConfigurado> beneficios = catalogo.listar();
        if (beneficios.isEmpty()) {
            return List.of();
        }
        ActualizacionFidelizacion estado = actualizacion.actualizarActual();
        return mapeador.beneficios(beneficios, estado.nivelActual(), estado.saldoPuntos(),
                canjesRegistrados(beneficios, movimientos));
    }

    /**
     * Canjes registrados de los beneficios que todavía podrían tener stock. Los deshabilitados o con stock inicial
     * cero ya están agotados y no necesitan consulta.
     */
    static Map<Long, Long> canjesRegistrados(List<BeneficioConfigurado> beneficios, AccesoMovimientosPuntos movimientos) {
        Set<Long> consultar = beneficios.stream()
                .filter(beneficio -> beneficio.estaHabilitado() && beneficio.stockInicial() > 0)
                .map(BeneficioConfigurado::id)
                .collect(Collectors.toSet());
        if (consultar.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> canjes = movimientos.exigir().contarCanjes(consultar);
        return canjes == null ? Map.of() : canjes;
    }
}
