package com.banduairlines.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.busqueda.AeropuertoBusquedaResponse;
import com.banduairlines.dto.busqueda.AlternativaVueloResponse;
import com.banduairlines.dto.busqueda.BusquedaVueloRequest;
import com.banduairlines.dto.busqueda.FiltroBusquedaRequest;
import com.banduairlines.dto.busqueda.OrdenVuelo;
import com.banduairlines.dto.busqueda.PasajerosBusquedaRequest;
import com.banduairlines.dto.busqueda.TarifaItinerarioResponse;
import com.banduairlines.dto.busqueda.TramoVueloResponse;
import com.banduairlines.dto.busqueda.VueloBusquedaResponse;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Aeropuerto;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.repository.AeropuertoRepository;
import com.banduairlines.service.busqueda.BusquedaService;
import com.banduairlines.service.busqueda.FiltroBusquedaService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BusquedaServiceImpl implements BusquedaService {

    private final AeropuertoRepository aeropuertos;
    private final FiltroBusquedaService filtros;
    private final Clock clock;

    @Override
    public List<VueloBusquedaResponse> buscarVuelos(BusquedaVueloRequest criterios,
                                                   PasajerosBusquedaRequest pasajeros) {
        var request = crearBusqueda(criterios, pasajeros);
        request.setOrdenarPor("horario_salida");
        var itinerarios = filtros.filtrarYOrdenar(request);
        var catalogo = catalogoAeropuertos();
        return itinerarios.stream().filter(i -> i.cantidadEscalas() == 0)
                .map(i -> mapearTramo(i.tramos().getFirst(), catalogo)).toList();
    }

    @Override
    public List<AlternativaVueloResponse> compararVuelos(BusquedaVueloRequest criterios, OrdenVuelo orden,
                                                        PasajerosBusquedaRequest pasajeros) {
        var request = crearBusqueda(criterios, pasajeros);
        switch (orden == null ? OrdenVuelo.HORARIO : orden) {
            case PRECIO_ASC -> request.setOrdenarPor("precio");
            case PRECIO_DESC -> {
                request.setOrdenarPor("precio");
                request.setOrden("desc");
            }
            case ESCALAS_ASC -> request.setOrdenarPor("escalas");
            case ESCALAS_DESC -> {
                request.setOrdenarPor("escalas");
                request.setOrden("desc");
            }
            case HORARIO -> request.setOrdenarPor("horario_salida");
        }
        var itinerarios = filtros.filtrarYOrdenar(request);
        var catalogo = catalogoAeropuertos();
        return itinerarios.stream().map(i -> new AlternativaVueloResponse(
                i.tramos().stream().map(t -> mapearTramo(t, catalogo)).toList(),
                i.tarifas().stream().map(TarifaItinerarioResponse::precioTotalTarifas)
                        .min(BigDecimal::compareTo).orElseThrow(),
                i.cantidadEscalas(), i.horarioSalida(), i.horarioLlegada(), i.duracionTotalMinutos())).toList();
    }

    @Override
    public List<AeropuertoBusquedaResponse> listarAeropuertos() {
        return aeropuertos.findAllByOrderByCiudadAscCodigoIataAsc().stream().map(this::mapear).toList();
    }

    private FiltroBusquedaRequest crearBusqueda(BusquedaVueloRequest criterios, PasajerosBusquedaRequest pasajeros) {
        if (criterios == null || criterios.origenId() == null || criterios.destinoId() == null
                || criterios.fecha() == null || criterios.origenId() <= 0 || criterios.destinoId() <= 0) {
            throw new ReglaNegocioException("Origen, destino y fecha válidos son obligatorios");
        }
        if (criterios.origenId().equals(criterios.destinoId())) {
            throw new ReglaNegocioException("El origen y el destino deben ser distintos");
        }
        Aeropuerto origen = obtenerAeropuerto(criterios.origenId());
        Aeropuerto destino = obtenerAeropuerto(criterios.destinoId());
        if (criterios.fecha().isBefore(LocalDate.now(clock))) {
            throw new ReglaNegocioException("La fecha de viaje no puede ser anterior al día actual");
        }
        if (criterios.fecha().equals(LocalDate.MAX)) {
            throw new ReglaNegocioException("La fecha de viaje está fuera del rango permitido");
        }
        var request = new FiltroBusquedaRequest();
        request.setOrigen(origen.getCodigoIata());
        request.setDestino(destino.getCodigoIata());
        request.setFecha(criterios.fecha());
        request.setCantidadAdultos(pasajeros.getCantidadAdultos());
        request.setCantidadNinos(pasajeros.getCantidadNinos());
        request.setCantidadBebes(pasajeros.getCantidadBebes());
        return request;
    }

    private Aeropuerto obtenerAeropuerto(Long id) {
        return aeropuertos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Aeropuerto", id));
    }

    private AeropuertoBusquedaResponse mapear(Aeropuerto a) {
        return new AeropuertoBusquedaResponse(a.getId(), a.getCodigoIata(), a.getNombre(),
                a.getCiudad(), a.getPais(), a.getZonaHoraria());
    }

    private Map<String, AeropuertoBusquedaResponse> catalogoAeropuertos() {
        return listarAeropuertos().stream().collect(Collectors.toMap(AeropuertoBusquedaResponse::codigoIata,
                Function.identity()));
    }

    private VueloBusquedaResponse mapearTramo(TramoVueloResponse tramo,
                                              Map<String, AeropuertoBusquedaResponse> catalogo) {
        return new VueloBusquedaResponse(tramo.vueloId(), tramo.codigoVuelo(), catalogo.get(tramo.origenCodigoIata()),
                catalogo.get(tramo.destinoCodigoIata()), tramo.salidaProgramada(), tramo.llegadaProgramada(),
                EstadoVuelo.valueOf(tramo.estadoVuelo()));
    }
}
