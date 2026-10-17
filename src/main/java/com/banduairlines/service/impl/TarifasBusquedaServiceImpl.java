package com.banduairlines.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.busqueda.ItinerarioVueloResponse;
import com.banduairlines.dto.busqueda.PackTarifarioBusquedaResponse;
import com.banduairlines.dto.busqueda.ResumenPasajerosResponse;
import com.banduairlines.dto.busqueda.TarifaItinerarioResponse;
import com.banduairlines.dto.busqueda.TarifasBusquedaRequest;
import com.banduairlines.dto.busqueda.TarifasBusquedaResponse;
import com.banduairlines.dto.busqueda.TramoTarifaResponse;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.repository.TarifaVueloRepository;
import com.banduairlines.repository.VueloRepository;
import com.banduairlines.service.busqueda.BusquedaVueloService;
import com.banduairlines.service.busqueda.PasajerosBusquedaService;
import com.banduairlines.service.busqueda.TarifasBusquedaService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TarifasBusquedaServiceImpl implements TarifasBusquedaService {

    private static final String MONEDA = "CLP";

    private final TarifaVueloRepository tarifaVueloRepository;
    private final VueloRepository vueloRepository;
    private final BusquedaVueloService busquedaVueloService;
    private final PasajerosBusquedaService pasajerosBusquedaService;

    @Override
    public TarifasBusquedaResponse consultar(TarifasBusquedaRequest request) {
        var pasajeros = pasajerosBusquedaService.validarYResumir(request);
        List<Long> ids = List.copyOf(request.getVueloIds());
        List<Vuelo> vuelos = ids.stream()
                .map(id -> vueloRepository.findByIdConDetalles(id)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Vuelo", id)))
                .toList();
        validarTramos(vuelos);
        var tarifasPorVuelo = cargarTarifas(ids, pasajeros);
        return new TarifasBusquedaResponse(ids, pasajeros, combinar(ids, tarifasPorVuelo, pasajeros));
    }

    @Override
    public List<ItinerarioVueloResponse> incorporarTarifas(
            List<ItinerarioVueloResponse> itinerarios, ResumenPasajerosResponse pasajeros) {
        if (itinerarios.isEmpty()) {
            return List.of();
        }
        List<Long> ids = itinerarios.stream().flatMap(i -> i.tramos().stream())
                .map(tramo -> tramo.vueloId()).distinct().toList();
        var tarifasPorVuelo = cargarTarifas(ids, pasajeros);
        return itinerarios.stream().map(itinerario -> {
            List<Long> vuelos = itinerario.tramos().stream().map(tramo -> tramo.vueloId()).toList();
            return itinerario.toBuilder().tarifas(combinar(vuelos, tarifasPorVuelo, pasajeros)).build();
        }).filter(i -> !i.tarifas().isEmpty()).toList();
    }

    private Map<Long, List<TarifaVuelo>> cargarTarifas(List<Long> ids, ResumenPasajerosResponse pasajeros) {
        Map<Long, List<TarifaVuelo>> catalogo = tarifaVueloRepository.buscarTarifasActivasPorVuelos(ids)
                .stream().collect(Collectors.groupingBy(t -> t.getVuelo().getId()));
        Map<Long, List<TarifaVuelo>> disponibles = new HashMap<>();
        for (Long id : ids) {
            List<TarifaVuelo> tarifas = catalogo.getOrDefault(id, List.of());
            if (tarifas.isEmpty()) {
                disponibles.put(id, List.of());
                continue;
            }
            Set<ClaseCabina> cabinas = busquedaVueloService
                    .obtenerClasesCabinaDisponibles(id, pasajeros.asientosNecesarios()).stream()
                    .filter(c -> c.disponible()).map(c -> ClaseCabina.valueOf(c.claseCabina()))
                    .collect(Collectors.toSet());
            disponibles.put(id, tarifas.stream().filter(t -> cabinas.contains(t.getClaseCabina())).toList());
        }
        return disponibles;
    }

    private List<TarifaItinerarioResponse> combinar(List<Long> ids, Map<Long, List<TarifaVuelo>> catalogo,
                                                   ResumenPasajerosResponse pasajeros) {
        List<TarifaItinerarioResponse> ofertas = new ArrayList<>();
        combinarTramo(0, ids, catalogo, new ArrayList<>(), pasajeros, ofertas);
        ofertas.sort(Comparator.comparing(TarifaItinerarioResponse::precioPorPasajero)
                .thenComparing(oferta -> oferta.pack().nombre()));
        return List.copyOf(ofertas);
    }

    private void combinarTramo(int posicion, List<Long> ids, Map<Long, List<TarifaVuelo>> catalogo,
                               List<TarifaVuelo> seleccion, ResumenPasajerosResponse pasajeros,
                               List<TarifaItinerarioResponse> ofertas) {
        if (posicion == ids.size()) {
            BigDecimal unitario = seleccion.stream().map(TarifaVuelo::getPrecio)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            List<TramoTarifaResponse> tramos = seleccion.stream().map(t -> new TramoTarifaResponse(
                    t.getVuelo().getId(), t.getVuelo().getCodigoVuelo(), t.getId(),
                    t.getClaseCabina(), t.getPrecio())).toList();
            ofertas.add(new TarifaItinerarioResponse(pack(seleccion.get(0).getPack()), MONEDA,
                    unitario, unitario.multiply(BigDecimal.valueOf(pasajeros.totalPasajeros())), tramos));
            return;
        }
        for (TarifaVuelo tarifa : catalogo.getOrDefault(ids.get(posicion), List.of())) {
            if (!seleccion.isEmpty() && !seleccion.get(0).getPack().getId().equals(tarifa.getPack().getId())) {
                continue;
            }
            seleccion.add(tarifa);
            combinarTramo(posicion + 1, ids, catalogo, seleccion, pasajeros, ofertas);
            seleccion.remove(seleccion.size() - 1);
        }
    }

    private void validarTramos(List<Vuelo> vuelos) {
        boolean horariosValidos = vuelos.stream().allMatch(v ->
                v.getLlegadaProgramada().isAfter(v.getSalidaProgramada()));
        if (!horariosValidos) {
            throw new ReglaNegocioException("Los horarios de los tramos no son válidos")
                    .conCodigo("ITINERARIO_INVALIDO");
        }
        if (vuelos.size() == 2) {
            Vuelo primero = vuelos.get(0);
            Vuelo segundo = vuelos.get(1);
            if (!primero.getRuta().getDestino().getId().equals(segundo.getRuta().getOrigen().getId())
                    || !segundo.getSalidaProgramada().isAfter(primero.getLlegadaProgramada())) {
                throw new ReglaNegocioException("Los vuelos no forman una conexión válida en el orden indicado")
                        .conCodigo("ITINERARIO_INVALIDO");
            }
        }
    }

    private PackTarifarioBusquedaResponse pack(PackTarifario p) {
        return new PackTarifarioBusquedaResponse(p.getId(), p.getNombre(), p.getDescripcion(),
                p.getIncluyeEquipajeCabina(), p.getPiezasBodegaIncluidas(), p.getKgPorPiezaBodega(),
                p.getIncluyeSeleccionAsiento(), p.getIncluyeEmbarquePrioritario(), p.getPermiteCambios(),
                p.getMultiplicadorMillas());
    }
}
