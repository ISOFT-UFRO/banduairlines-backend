package com.banduairlines.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.busqueda.BusquedaItinerarioRequest;
import com.banduairlines.dto.busqueda.BusquedaItinerarioResponse;
import com.banduairlines.service.busqueda.BusquedaVueloService;
import com.banduairlines.service.busqueda.TarifasBusquedaService;
import com.banduairlines.service.busqueda.ItinerarioBusquedaService;
import com.banduairlines.service.busqueda.PasajerosBusquedaService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItinerarioBusquedaServiceImpl implements ItinerarioBusquedaService {

    private final BusquedaVueloService busquedaVueloService;
    private final PasajerosBusquedaService pasajerosBusquedaService;
    private final TarifasBusquedaService tarifasBusquedaService;
    private final Clock clock;

    @Override
    public BusquedaItinerarioResponse buscar(BusquedaItinerarioRequest request) {
        var pasajeros = pasajerosBusquedaService.validarYResumir(request);
        String origen = request.getOrigen().toUpperCase(Locale.ROOT);
        String destino = request.getDestino().toUpperCase(Locale.ROOT);
        LocalDateTime ahora = LocalDateTime.now(clock);

        var itinerarios = busquedaVueloService.obtenerAlternativasItinerario(origen, destino, request.getFecha())
                .stream()
                .filter(i -> i.horarioSalida().isAfter(ahora))
                .filter(i -> !i.tramos().isEmpty() && i.tramos().stream()
                        .allMatch(tramo -> tramo.duracionMinutos() > 0))
                .toList();

        return new BusquedaItinerarioResponse(origen, destino, request.getFecha(), pasajeros,
                tarifasBusquedaService.incorporarTarifas(itinerarios, pasajeros));
    }
}
