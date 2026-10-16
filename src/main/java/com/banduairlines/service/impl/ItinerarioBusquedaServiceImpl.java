package com.banduairlines.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.busqueda.BusquedaItinerarioRequest;
import com.banduairlines.dto.busqueda.BusquedaItinerarioResponse;
import com.banduairlines.dto.busqueda.ItinerarioVueloResponse;
import com.banduairlines.dto.busqueda.TramoVueloResponse;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.repository.VueloRepository;
import com.banduairlines.service.busqueda.ItinerarioBusquedaService;
import com.banduairlines.service.busqueda.PasajerosBusquedaService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItinerarioBusquedaServiceImpl implements ItinerarioBusquedaService {

    private static final List<EstadoVuelo> ESTADOS_VENDIBLES =
            List.of(EstadoVuelo.PROGRAMADO, EstadoVuelo.RETRASADO);

    private final VueloRepository vueloRepository;
    private final PasajerosBusquedaService pasajerosBusquedaService;
    private final Clock clock;

    @Override
    public BusquedaItinerarioResponse buscar(BusquedaItinerarioRequest request) {
        var pasajeros = pasajerosBusquedaService.validarYResumir(request);
        String origen = request.getOrigen().toUpperCase(Locale.ROOT);
        String destino = request.getDestino().toUpperCase(Locale.ROOT);
        LocalDateTime inicio = request.getFecha().atStartOfDay();
        LocalDateTime fin = inicio.plusDays(1);
        LocalDateTime ahora = LocalDateTime.now(clock);

        // El reloj compartido del proyecto utiliza UTC. No se ofrecen salidas pasadas.
        if (inicio.isBefore(ahora)) {
            inicio = ahora;
        }
        if (!inicio.isBefore(fin)) {
            return new BusquedaItinerarioResponse(origen, destino, request.getFecha(), pasajeros, List.of());
        }

        List<ItinerarioVueloResponse> itinerarios = new ArrayList<>();
        vueloRepository.buscarVuelosDirectos(origen, destino, inicio, fin, ESTADOS_VENDIBLES).stream()
                .filter(this::horariosValidos)
                .map(this::directo)
                .forEach(itinerarios::add);

        List<Vuelo> primeros = vueloRepository
                .buscarPrimerosTramosParaConexion(origen, destino, inicio, fin, ESTADOS_VENDIBLES)
                .stream().filter(this::horariosValidos).toList();
        agregarConexiones(origen, destino, primeros, itinerarios);

        itinerarios.sort(Comparator.comparing(ItinerarioVueloResponse::horarioSalida)
                .thenComparingLong(ItinerarioVueloResponse::duracionTotalMinutos));
        return new BusquedaItinerarioResponse(origen, destino, request.getFecha(), pasajeros,
                List.copyOf(itinerarios));
    }

    private void agregarConexiones(String origen, String destino, List<Vuelo> primeros,
                                   List<ItinerarioVueloResponse> itinerarios) {
        if (primeros.isEmpty()) {
            return;
        }
        LocalDateTime inicioVentana = primeros.stream().map(Vuelo::getLlegadaProgramada)
                .min(Comparator.naturalOrder()).orElseThrow();
        List<Vuelo> segundos = vueloRepository.buscarSegundosTramosParaConexion(
                origen, destino, inicioVentana, null, ESTADOS_VENDIBLES);

        for (Vuelo primero : primeros) {
            for (Vuelo segundo : segundos) {
                if (!horariosValidos(segundo)
                        || !primero.getRuta().getDestino().getId().equals(segundo.getRuta().getOrigen().getId())
                        || !segundo.getSalidaProgramada().isAfter(primero.getLlegadaProgramada())) {
                    continue;
                }
                var escala = primero.getRuta().getDestino();
                itinerarios.add(ItinerarioVueloResponse.builder()
                        .tipoItinerario("CONEXION")
                        .cantidadEscalas(1)
                        .aeropuertoEscalaIata(escala.getCodigoIata())
                        .ciudadEscala(escala.getCiudad())
                        .tiempoEscalaMinutos(Duration.between(primero.getLlegadaProgramada(),
                                segundo.getSalidaProgramada()).toMinutes())
                        .horarioSalida(primero.getSalidaProgramada())
                        .horarioLlegada(segundo.getLlegadaProgramada())
                        .duracionTotalMinutos(Duration.between(primero.getSalidaProgramada(),
                                segundo.getLlegadaProgramada()).toMinutes())
                        .tramos(List.of(tramo(primero), tramo(segundo)))
                        .build());
            }
        }
    }

    private boolean horariosValidos(Vuelo vuelo) {
        return vuelo.getLlegadaProgramada().isAfter(vuelo.getSalidaProgramada());
    }

    private ItinerarioVueloResponse directo(Vuelo vuelo) {
        return ItinerarioVueloResponse.builder()
                .tipoItinerario("DIRECTO")
                .cantidadEscalas(0)
                .horarioSalida(vuelo.getSalidaProgramada())
                .horarioLlegada(vuelo.getLlegadaProgramada())
                .duracionTotalMinutos(Duration.between(vuelo.getSalidaProgramada(),
                        vuelo.getLlegadaProgramada()).toMinutes())
                .tramos(List.of(tramo(vuelo)))
                .build();
    }

    private TramoVueloResponse tramo(Vuelo vuelo) {
        var origen = vuelo.getRuta().getOrigen();
        var destino = vuelo.getRuta().getDestino();
        return TramoVueloResponse.builder()
                .vueloId(vuelo.getId())
                .codigoVuelo(vuelo.getCodigoVuelo())
                .origenCodigoIata(origen.getCodigoIata())
                .origenCiudad(origen.getCiudad())
                .origenNombre(origen.getNombre())
                .destinoCodigoIata(destino.getCodigoIata())
                .destinoCiudad(destino.getCiudad())
                .destinoNombre(destino.getNombre())
                .salidaProgramada(vuelo.getSalidaProgramada())
                .llegadaProgramada(vuelo.getLlegadaProgramada())
                .duracionMinutos(Duration.between(vuelo.getSalidaProgramada(),
                        vuelo.getLlegadaProgramada()).toMinutes())
                .aeronaveModelo(vuelo.getAeronave().getModelo())
                .estadoVuelo(vuelo.getEstado().name())
                .build();
    }
}
