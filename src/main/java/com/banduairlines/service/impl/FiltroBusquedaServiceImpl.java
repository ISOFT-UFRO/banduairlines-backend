package com.banduairlines.service.impl;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.busqueda.FiltroBusquedaRequest;
import com.banduairlines.dto.busqueda.ItinerarioVueloResponse;
import com.banduairlines.dto.busqueda.TarifaItinerarioResponse;
import com.banduairlines.service.busqueda.FiltroBusquedaService;
import com.banduairlines.service.busqueda.ItinerarioBusquedaService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FiltroBusquedaServiceImpl implements FiltroBusquedaService {

    private final ItinerarioBusquedaService itinerarioBusquedaService;

    @Override
    public List<ItinerarioVueloResponse> filtrarYOrdenar(FiltroBusquedaRequest request) {
        return itinerarioBusquedaService.buscar(request).itinerarios().stream()
                .filter(i -> request.getMaxEscalas() == null || i.cantidadEscalas() <= request.getMaxEscalas())
                .filter(i -> request.getDuracionMaximaMinutos() == null
                        || i.duracionTotalMinutos() <= request.getDuracionMaximaMinutos())
                .filter(i -> dentroDeHorario(i.horarioSalida().toLocalTime(), request))
                .map(i -> i.toBuilder().tarifas(i.tarifas().stream()
                        .filter(t -> dentroDePrecio(t.precioTotalTarifas(), request)).toList()).build())
                .filter(i -> !i.tarifas().isEmpty())
                .sorted(ordenar(request))
                .toList();
    }

    private boolean dentroDePrecio(BigDecimal precio, FiltroBusquedaRequest request) {
        return (request.getPrecioMinimo() == null || precio.compareTo(request.getPrecioMinimo()) >= 0)
                && (request.getPrecioMaximo() == null || precio.compareTo(request.getPrecioMaximo()) <= 0);
    }

    private boolean dentroDeHorario(LocalTime hora, FiltroBusquedaRequest request) {
        LocalTime minimo = request.getHorarioSalidaMinimo();
        LocalTime maximo = request.getHorarioSalidaMaximo();
        if (minimo != null && maximo != null && minimo.isAfter(maximo)) {
            return !hora.isBefore(minimo) || !hora.isAfter(maximo);
        }
        return (minimo == null || !hora.isBefore(minimo)) && (maximo == null || !hora.isAfter(maximo));
    }

    private Comparator<ItinerarioVueloResponse> ordenar(FiltroBusquedaRequest request) {
        Comparator<ItinerarioVueloResponse> principal = switch (request.getOrdenarPor()) {
            case "horario_salida" -> Comparator.comparing(ItinerarioVueloResponse::horarioSalida);
            case "duracion" -> Comparator.comparingLong(ItinerarioVueloResponse::duracionTotalMinutos);
            case "escalas" -> Comparator.comparingInt(ItinerarioVueloResponse::cantidadEscalas);
            default -> Comparator.comparing(this::precioDesde);
        };
        if ("desc".equals(request.getOrden())) {
            principal = principal.reversed();
        }
        return principal.thenComparing(ItinerarioVueloResponse::horarioSalida)
                .thenComparing(i -> i.tramos().getFirst().vueloId());
    }

    private BigDecimal precioDesde(ItinerarioVueloResponse itinerario) {
        return itinerario.tarifas().stream().map(TarifaItinerarioResponse::precioTotalTarifas)
                .min(BigDecimal::compareTo).orElseThrow();
    }
}
