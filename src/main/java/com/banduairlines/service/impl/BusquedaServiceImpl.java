package com.banduairlines.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.banduairlines.dto.busqueda.AeropuertoBusquedaResponse;
import com.banduairlines.dto.busqueda.BusquedaVueloRequest;
import com.banduairlines.dto.busqueda.VueloBusquedaResponse;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Aeropuerto;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.Estado;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.repository.AeropuertoRepository;
import com.banduairlines.repository.VueloRepository;
import com.banduairlines.service.BusquedaService;

@Service
@Transactional(readOnly = true)
public class BusquedaServiceImpl implements BusquedaService {
    private final VueloRepository vuelos;
    private final AeropuertoRepository aeropuertos;
    private final Clock clock;

    public BusquedaServiceImpl(VueloRepository vuelos, AeropuertoRepository aeropuertos, Clock clock) {
        this.vuelos = vuelos;
        this.aeropuertos = aeropuertos;
        this.clock = clock;
    }

    @Override
    public List<VueloBusquedaResponse> buscarVuelos(BusquedaVueloRequest criterios) {
        if (criterios == null || criterios.origenId() == null || criterios.destinoId() == null
                || criterios.fecha() == null || criterios.origenId() <= 0 || criterios.destinoId() <= 0) {
            throw new ReglaNegocioException("Origen, destino y fecha válidos son obligatorios");
        }
        if (criterios.origenId().equals(criterios.destinoId())) {
            throw new ReglaNegocioException("El origen y el destino deben ser distintos");
        }
        Aeropuerto origen = obtenerAeropuerto(criterios.origenId());
        obtenerAeropuerto(criterios.destinoId());
        // Los LocalDateTime de salida se interpretan como hora local del aeropuerto de origen.
        LocalDateTime ahora = LocalDateTime.ofInstant(clock.instant(), ZoneId.of(origen.getZonaHoraria()));
        if (criterios.fecha().isBefore(ahora.toLocalDate())) {
            throw new ReglaNegocioException("La fecha de viaje no puede ser anterior al día actual en el origen");
        }
        if (criterios.fecha().equals(LocalDate.MAX)) {
            throw new ReglaNegocioException("La fecha de viaje está fuera del rango permitido");
        }
        List<Vuelo> encontrados = vuelos.buscarOfrecibles(criterios.origenId(), criterios.destinoId(),
                criterios.fecha().atStartOfDay(), criterios.fecha().plusDays(1).atStartOfDay(), ahora,
                List.of(EstadoVuelo.PROGRAMADO, EstadoVuelo.RETRASADO), Estado.ACTIVA);
        if (encontrados.isEmpty()) {
            return List.of();
        }
        return encontrados.stream().map(v -> new VueloBusquedaResponse(v.getId(), v.getCodigoVuelo(),
                        mapear(v.getRuta().getOrigen()), mapear(v.getRuta().getDestino()),
                        v.getSalidaProgramada(), v.getLlegadaProgramada(), v.getEstado())).toList();
    }

    @Override
    public List<AeropuertoBusquedaResponse> listarAeropuertos() {
        return aeropuertos.findAllByOrderByCiudadAscCodigoIataAsc().stream()
                .map(this::mapear).toList();
    }

    private Aeropuerto obtenerAeropuerto(Long id) {
        return aeropuertos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Aeropuerto", id));
    }

    private AeropuertoBusquedaResponse mapear(Aeropuerto a) {
        return new AeropuertoBusquedaResponse(a.getId(), a.getCodigoIata(), a.getNombre(),
                a.getCiudad(), a.getPais(), a.getZonaHoraria());
    }
}
