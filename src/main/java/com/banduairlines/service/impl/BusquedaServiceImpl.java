package com.banduairlines.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.banduairlines.dto.busqueda.AeropuertoBusquedaResponse;
import com.banduairlines.dto.busqueda.AlternativaVueloResponse;
import com.banduairlines.dto.busqueda.BusquedaVueloRequest;
import com.banduairlines.dto.busqueda.OrdenVuelo;
import com.banduairlines.dto.busqueda.VueloBusquedaResponse;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Aeropuerto;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.Estado;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.repository.AeropuertoRepository;
import com.banduairlines.repository.TarifaVueloRepository;
import com.banduairlines.repository.VueloRepository;
import com.banduairlines.service.busqueda.BusquedaService;

@Service
@Transactional(readOnly = true)
public class BusquedaServiceImpl implements BusquedaService {
    private static final Duration CONEXION_MINIMA = Duration.ofMinutes(60);
    private static final Duration CONEXION_MAXIMA = Duration.ofHours(24);
    private final VueloRepository vuelos;
    private final AeropuertoRepository aeropuertos;
    private final TarifaVueloRepository tarifas;
    private final Clock clock;

    public BusquedaServiceImpl(VueloRepository vuelos, AeropuertoRepository aeropuertos,
            TarifaVueloRepository tarifas, Clock clock) {
        this.vuelos = vuelos;
        this.aeropuertos = aeropuertos;
        this.tarifas = tarifas;
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
    public List<AlternativaVueloResponse> compararVuelos(BusquedaVueloRequest criterios, OrdenVuelo orden) {
        // Reutiliza las validaciones y reglas de fecha/origen de RF-01.
        buscarVuelos(criterios);
        if (criterios.fecha().isAfter(LocalDate.MAX.minusDays(3))) {
            throw new ReglaNegocioException("La fecha de viaje está fuera del rango permitido");
        }
        List<Vuelo> candidatos = vuelos.buscarCandidatosOfrecibles(
                criterios.fecha().minusDays(1).atStartOfDay(),
                criterios.fecha().plusDays(3).atStartOfDay(),
                List.of(EstadoVuelo.PROGRAMADO, EstadoVuelo.RETRASADO), Estado.ACTIVA);
        if (candidatos.isEmpty()) {
            return List.of();
        }

        Map<Long, BigDecimal> precios = new HashMap<>();
        tarifas.buscarPreciosMinimos(candidatos.stream().map(Vuelo::getId).toList())
                .forEach(p -> precios.put(p.getVueloId(), p.getPrecioDesde()));
        Map<Long, List<Vuelo>> porOrigen = new HashMap<>();
        candidatos.forEach(v -> porOrigen.computeIfAbsent(v.getRuta().getOrigen().getId(), ignored -> new ArrayList<>()).add(v));

        List<AlternativaVueloResponse> alternativas = new ArrayList<>();
        for (Vuelo primerTramo : porOrigen.getOrDefault(criterios.origenId(), List.of())) {
            if (!primerTramo.getSalidaProgramada().toLocalDate().equals(criterios.fecha())) {
                continue;
            }
            if (!instante(primerTramo.getSalidaProgramada(), primerTramo.getRuta().getOrigen().getZonaHoraria())
                    .isAfter(clock.instant())) {
                continue;
            }
            BigDecimal precioPrimerTramo = precios.get(primerTramo.getId());
            if (precioPrimerTramo == null) {
                continue;
            }
            if (primerTramo.getRuta().getDestino().getId().equals(criterios.destinoId())) {
                alternativas.add(alternativa(List.of(primerTramo), precioPrimerTramo));
            }

            Long aeropuertoConexion = primerTramo.getRuta().getDestino().getId();
            if (aeropuertoConexion.equals(criterios.origenId()) || aeropuertoConexion.equals(criterios.destinoId())) {
                continue;
            }
            Instant llegadaPrimerTramo = instante(primerTramo.getLlegadaProgramada(),
                    primerTramo.getRuta().getDestino().getZonaHoraria());
            for (Vuelo segundoTramo : porOrigen.getOrDefault(aeropuertoConexion, List.of())) {
                if (!segundoTramo.getRuta().getDestino().getId().equals(criterios.destinoId())) {
                    continue;
                }
                BigDecimal precioSegundoTramo = precios.get(segundoTramo.getId());
                if (precioSegundoTramo == null) {
                    continue;
                }
                Instant salidaSegundoTramo = instante(segundoTramo.getSalidaProgramada(),
                        segundoTramo.getRuta().getOrigen().getZonaHoraria());
                Duration espera = Duration.between(llegadaPrimerTramo, salidaSegundoTramo);
                if (espera.compareTo(CONEXION_MINIMA) >= 0 && espera.compareTo(CONEXION_MAXIMA) <= 0) {
                    alternativas.add(alternativa(List.of(primerTramo, segundoTramo),
                            precioPrimerTramo.add(precioSegundoTramo)));
                }
            }
        }
        return ordenar(alternativas, orden);
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

    private AlternativaVueloResponse alternativa(List<Vuelo> tramos, BigDecimal precio) {
        Vuelo primero = tramos.getFirst();
        Vuelo ultimo = tramos.getLast();
        Instant salida = instante(primero.getSalidaProgramada(), primero.getRuta().getOrigen().getZonaHoraria());
        Instant llegada = instante(ultimo.getLlegadaProgramada(), ultimo.getRuta().getDestino().getZonaHoraria());
        return new AlternativaVueloResponse(tramos.stream().map(v -> new VueloBusquedaResponse(
                v.getId(), v.getCodigoVuelo(), mapear(v.getRuta().getOrigen()), mapear(v.getRuta().getDestino()),
                v.getSalidaProgramada(), v.getLlegadaProgramada(), v.getEstado())).toList(),
                precio, tramos.size() - 1, primero.getSalidaProgramada(), ultimo.getLlegadaProgramada(),
                Duration.between(salida, llegada).toMinutes());
    }

    private Instant instante(LocalDateTime horario, String zonaHoraria) {
        return horario.atZone(ZoneId.of(zonaHoraria)).toInstant();
    }

    private List<AlternativaVueloResponse> ordenar(List<AlternativaVueloResponse> alternativas, OrdenVuelo orden) {
        Comparator<AlternativaVueloResponse> porSalida = Comparator.comparing(AlternativaVueloResponse::salidaProgramada);
        Comparator<AlternativaVueloResponse> porPrecio = Comparator.comparing(AlternativaVueloResponse::precioDesde)
                .thenComparing(porSalida);
        Comparator<AlternativaVueloResponse> porEscalas = Comparator.comparingInt(AlternativaVueloResponse::cantidadEscalas)
                .thenComparing(porSalida);
        Comparator<AlternativaVueloResponse> comparador = switch (orden == null ? OrdenVuelo.HORARIO : orden) {
            case PRECIO_ASC -> porPrecio;
            case PRECIO_DESC -> Comparator.comparing(AlternativaVueloResponse::precioDesde).reversed().thenComparing(porSalida);
            case ESCALAS_ASC -> porEscalas;
            case ESCALAS_DESC -> Comparator.comparingInt(AlternativaVueloResponse::cantidadEscalas).reversed().thenComparing(porSalida);
            case HORARIO -> porSalida;
        };
        return alternativas.stream().sorted(comparador).toList();
    }
}
