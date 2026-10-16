package com.banduairlines.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.busqueda.BusquedaItinerarioRequest;
import com.banduairlines.dto.busqueda.BusquedaItinerarioResponse;
import com.banduairlines.service.busqueda.BusquedaVueloService;
import com.banduairlines.service.busqueda.ItinerarioBusquedaService;
import com.banduairlines.service.busqueda.PasajerosBusquedaService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItinerarioBusquedaServiceImpl implements ItinerarioBusquedaService {

    private final BusquedaVueloService busquedaVueloService;
    private final PasajerosBusquedaService pasajerosBusquedaService;
    private final Clock clock;

    @Override
    public BusquedaItinerarioResponse buscar(BusquedaItinerarioRequest request) {
        var pasajeros = pasajerosBusquedaService.validarYResumir(request);
        String origen = request.getOrigen().toUpperCase(Locale.ROOT);
        String destino = request.getDestino().toUpperCase(Locale.ROOT);
        LocalDateTime ahora = LocalDateTime.now(clock);
        Map<Long, Boolean> cuposPorVuelo = new HashMap<>();

        var itinerarios = busquedaVueloService.obtenerAlternativasItinerario(origen, destino, request.getFecha())
                .stream()
                .filter(i -> i.horarioSalida().isAfter(ahora))
                .filter(i -> !i.tramos().isEmpty() && i.tramos().stream().allMatch(tramo ->
                        cuposPorVuelo.computeIfAbsent(tramo.vueloId(), vueloId ->
                                busquedaVueloService.obtenerClasesCabinaDisponibles(
                                        vueloId, pasajeros.asientosNecesarios())
                                        .stream().anyMatch(cabina -> cabina.disponible()))))
                .toList();

        return new BusquedaItinerarioResponse(origen, destino, request.getFecha(), pasajeros, itinerarios);
    }
}
