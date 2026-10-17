package com.banduairlines.service.reserva;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

import com.banduairlines.model.Asiento;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.BoletoExtra;
import com.banduairlines.model.Extra;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.enums.TipoExtra;
import com.banduairlines.repository.CatalogoReservaRepository;

/**
 * Reglas de asientos de RESERVA
 * 
 * Precio: el asiento normal no tiene costo. El preferente cobra el Extra ASIENTO_PREFERENTE del catalogo
 * de ADMIN solo si el pack no incluye selección de asiento; con seleccion incluida es gratis
 * 
 * Distribución: las columnas salen de las letras de la aeronave; el pasillo va al medio (A B C | D E F)
 * 
 */
@Component
public class AsientosReserva {

    public static final String CATEGORIA_PREFERENTE = "PREFERENTE";
    public static final String CATEGORIA_ESTANDAR = "ESTANDAR";

    private final CatalogoReservaRepository catalogoReservaRepository;

    public AsientosReserva(CatalogoReservaRepository catalogoReservaRepository) {
        this.catalogoReservaRepository = catalogoReservaRepository;
    }

    /** Precio vigente del asiento preferente en el catalogo (el primero activo), si existe. */
    public Optional<Extra> precioPreferente() {
        return catalogoReservaRepository.extrasActivos(TipoExtra.ASIENTO_PREFERENTE).stream().findFirst();
    }

    /**
     * Lo que cuesta elegir el asiento con este pack. Optional.empty() = es un preferente que se cobra
     * pero falta su precio en el catalogo, asi que no se puede vender.
     */
    public static Optional<Long> precio(Asiento asiento, PackTarifario pack, Optional<Extra> preferente) {
        if (incluidoEnPack(asiento, pack)) {
            return Optional.of(0L);
        }
        return preferente.map(e -> CalculadoraTotalReserva.clp(e.getPrecio()));
    }

    /** El asiento no tiene costo adicional para este pack. */
    public static boolean incluidoEnPack(Asiento asiento, PackTarifario pack) {
        return !Boolean.TRUE.equals(asiento.getEsPreferente()) || Boolean.TRUE.equals(pack.getIncluyeSeleccionAsiento());
    }

    public static String categoria(Asiento asiento) {
        return Boolean.TRUE.equals(asiento.getEsPreferente()) ? CATEGORIA_PREFERENTE : CATEGORIA_ESTANDAR;
    }

    public static String codigo(Asiento asiento) {
        return asiento.getFila() + asiento.getLetra();
    }

    /** Letras de la aeronave en orden (A, B, C, ...). */
    public static List<String> columnas(List<Asiento> asientos) {
        Set<String> letras = new TreeSet<>();
        asientos.forEach(a -> letras.add(a.getLetra()));
        return List.copyOf(letras);
    }

    /** ADMIN no guarda pasillos: con columnas pares se asume un pasillo central. */
    public static List<String> pasillosDespuesDe(List<String> columnas) {
        if (columnas.size() < 4 || columnas.size() % 2 != 0) {
            return List.of();
        }
        return List.of(columnas.get(columnas.size() / 2 - 1));
    }

    /**
     * true si todos los asientos están en la misma fila, uno al lado del otro y sin un pasillo entre medio.
     * La vecindad se mide con las letras que realmente tiene esa fila (en BUSINESS, A y C son vecinos).
     */
    public static boolean contiguos(List<Asiento> grupo, List<Asiento> asientosDeLaAeronave) {
        if (grupo.size() <= 1) {
            return true;
        }
        Integer fila = grupo.get(0).getFila();
        if (grupo.stream().anyMatch(a -> !a.getFila().equals(fila))) {
            return false;
        }
        List<String> letrasFila = asientosDeLaAeronave.stream()
                .filter(a -> a.getFila().equals(fila))
                .map(Asiento::getLetra)
                .sorted()
                .toList();
        List<String> pasillos = pasillosDespuesDe(columnas(asientosDeLaAeronave));
        List<Integer> posiciones = grupo.stream()
                .map(a -> letrasFila.indexOf(a.getLetra()))
                .sorted(Comparator.naturalOrder())
                .toList();
        for (int i = 1; i < posiciones.size(); i++) {
            int anterior = posiciones.get(i - 1);
            if (posiciones.get(i) != anterior + 1 || pasillos.contains(letrasFila.get(anterior))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Deja el boleto con este asiento (o sin asiento si es null) y con su cobro: cobro es el Extra
     * ASIENTO_PREFERENTE si el asiento se cobra, o null si no tiene costo. El cobro que se mantiene se actualiza
     * en el mismo registro: Hibernate inserta antes de borrar, así que borrar y volver a crear la misma clave
     * compuesta fallaría (ver ReservaServiciosServiceImpl).
     */
    public static void asignar(Boleto boleto, Asiento asiento, Extra cobro) {
        boleto.setAsientoId(asiento);
        Extra pendiente = asiento == null ? null : cobro;
        Iterator<BoletoExtra> it = boleto.getExtras().iterator();
        while (it.hasNext()) {
            BoletoExtra actual = it.next();
            if (actual.getExtra().getTipo() != TipoExtra.ASIENTO_PREFERENTE) {
                continue;
            }
            if (pendiente != null && pendiente.getId().equals(actual.getExtra().getId())) {
                actual.setCantidad(1);
                actual.setPrecioUnitario(pendiente.getPrecio());
                pendiente = null;
            } else {
                it.remove();
            }
        }
        if (pendiente != null) {
            boleto.addExtra(pendiente, 1, pendiente.getPrecio());
        }
    }

    /** Deja el boleto sin asiento y sin el cobro de asiento preferente (cancelacion). */
    public static void liberar(Boleto boleto) {
        boleto.setAsientoId(null);
        boleto.getExtras().removeIf(be -> be.getExtra().getTipo() == TipoExtra.ASIENTO_PREFERENTE);
    }
}
