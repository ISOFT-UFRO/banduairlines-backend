package com.banduairlines.service.cuenta;

import com.banduairlines.model.enums.Nivel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Catálogo de beneficios definido en la configuración. Se valida al iniciar la aplicación:
 * si algún beneficio está mal configurado, la aplicación no arranca y el mensaje indica cuál y por qué.
 * No existe un endpoint para modificarlo.
 */
@Component
public class CatalogoBeneficios {

    static final int LARGO_MAXIMO_NOMBRE = 60;
    private static final int LARGO_MAXIMO_DESCRIPCION = 200;

    private final List<BeneficioConfigurado> beneficios;

    public CatalogoBeneficios(PropiedadesFidelizacion propiedades) {
        this.beneficios = validar(propiedades.beneficios()).stream()
                .sorted(Comparator.comparing(BeneficioConfigurado::costoPuntos)
                        .thenComparing(BeneficioConfigurado::nombre))
                .toList();
    }

    /** Todos los beneficios configurados, del más barato al más caro. Puede ser una lista vacía. */
    public List<BeneficioConfigurado> listar() {
        return beneficios;
    }

    public Optional<BeneficioConfigurado> buscar(Long idBeneficio) {
        return beneficios.stream()
                .filter(beneficio -> beneficio.id().equals(idBeneficio))
                .findFirst();
    }

    private static List<BeneficioConfigurado> validar(List<BeneficioConfigurado> configurados) {
        List<String> errores = new ArrayList<>();
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < configurados.size(); i++) {
            BeneficioConfigurado b = configurados.get(i);
            String donde = "app.fidelizacion.beneficios[" + i + "]";
            if (b.id() == null || b.id() <= 0) {
                errores.add(donde + ".id debe ser positivo");
            } else if (!ids.add(b.id())) {
                errores.add(donde + ".id " + b.id() + " está repetido");
            }
            if (b.nombre() == null || b.nombre().isBlank()) {
                errores.add(donde + ".nombre es obligatorio");
            } else if (b.nombre().length() > LARGO_MAXIMO_NOMBRE) {
                errores.add(donde + ".nombre no puede superar " + LARGO_MAXIMO_NOMBRE + " caracteres");
            }
            if (b.descripcion() != null && b.descripcion().length() > LARGO_MAXIMO_DESCRIPCION) {
                errores.add(donde + ".descripcion no puede superar " + LARGO_MAXIMO_DESCRIPCION + " caracteres");
            }
            if (b.nivelRequerido() == null || b.nivelRequerido() == Nivel.PLATINO) {
                errores.add(donde + ".nivel-requerido debe ser BASICO, PLATA u ORO");
            }
            if (b.costoPuntos() == null || b.costoPuntos() <= 0) {
                errores.add(donde + ".costo-puntos debe ser positivo");
            }
            if (b.stockInicial() == null || b.stockInicial() < 0) {
                errores.add(donde + ".stock-inicial no puede ser negativo");
            }
        }
        if (!errores.isEmpty()) {
            throw new IllegalStateException("Catálogo de beneficios inválido: " + String.join("; ", errores));
        }
        return List.copyOf(configurados);
    }
}
