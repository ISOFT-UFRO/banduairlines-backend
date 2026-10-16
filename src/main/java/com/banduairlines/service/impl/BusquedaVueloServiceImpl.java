package com.banduairlines.service.impl;

import com.banduairlines.dto.busqueda.ItinerarioVueloResponse;
import com.banduairlines.dto.busqueda.TramoVueloResponse;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Aeropuerto;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.repository.VueloRepository;
import com.banduairlines.service.busqueda.BusquedaVueloService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BusquedaVueloServiceImpl implements BusquedaVueloService {

    private static final long MIN_TIEMPO_ESCALA_MINUTOS = 45L;
    private static final long MAX_TIEMPO_ESCALA_MINUTOS = 720L;
    private static final Set<EstadoVuelo> ESTADOS_DISPONIBLES = EnumSet.of(
            EstadoVuelo.PROGRAMADO,
            EstadoVuelo.RETRASADO
    );

    private final VueloRepository vueloRepository;

    public BusquedaVueloServiceImpl(VueloRepository vueloRepository) {
        this.vueloRepository = vueloRepository;
    }

    @Override
    public List<ItinerarioVueloResponse> obtenerAlternativasItinerario(
            String origenIata,
            String destinoIata,
            LocalDate fechaSalida
    ) {
        String origenNormalizado = origenIata.trim().toUpperCase();
        String destinoNormalizado = destinoIata.trim().toUpperCase();

        if (origenNormalizado.equals(destinoNormalizado)) {
            throw new ReglaNegocioException("El aeropuerto de origen y destino no pueden ser iguales")
                    .conCodigo("ORIGEN_DESTINO_IGUALES");
        }

        LocalDateTime inicioDia = fechaSalida.atStartOfDay();
        LocalDateTime finDia = fechaSalida.plusDays(1).atStartOfDay();

        List<ItinerarioVueloResponse> alternativas = new ArrayList<>();

        List<Vuelo> vuelosDirectos = vueloRepository.buscarVuelosDirectos(
                origenNormalizado,
                destinoNormalizado,
                inicioDia,
                finDia,
                ESTADOS_DISPONIBLES
        );

        for (Vuelo directo : vuelosDirectos) {
            alternativas.add(construirItinerarioDirecto(directo));
        }

        List<Vuelo> primerosTramos = vueloRepository.buscarPrimerosTramosParaConexion(
                origenNormalizado,
                destinoNormalizado,
                inicioDia,
                finDia,
                ESTADOS_DISPONIBLES
        );

        if (!primerosTramos.isEmpty()) {
            LocalDateTime primeraLlegada = primerosTramos.stream()
                    .map(Vuelo::getLlegadaProgramada)
                    .min(LocalDateTime::compareTo)
                    .orElse(inicioDia);

            LocalDateTime ultimaLlegada = primerosTramos.stream()
                    .map(Vuelo::getLlegadaProgramada)
                    .max(LocalDateTime::compareTo)
                    .orElse(finDia);

            List<Vuelo> segundosTramosCandidatos = vueloRepository.buscarSegundosTramosParaConexion(
                    origenNormalizado,
                    destinoNormalizado,
                    primeraLlegada.plusMinutes(MIN_TIEMPO_ESCALA_MINUTOS),
                    ultimaLlegada.plusMinutes(MAX_TIEMPO_ESCALA_MINUTOS),
                    ESTADOS_DISPONIBLES
            );

            for (Vuelo tramo1 : primerosTramos) {
                String codigoEscala = tramo1.getRuta().getDestino().getCodigoIata();

                for (Vuelo tramo2 : segundosTramosCandidatos) {
                    if (!tramo2.getRuta().getOrigen().getCodigoIata().equalsIgnoreCase(codigoEscala)) {
                        continue;
                    }

                    long minutosEscala = Duration.between(
                            tramo1.getLlegadaProgramada(),
                            tramo2.getSalidaProgramada()
                    ).toMinutes();

                    if (minutosEscala >= MIN_TIEMPO_ESCALA_MINUTOS && minutosEscala <= MAX_TIEMPO_ESCALA_MINUTOS) {
                        alternativas.add(construirItinerarioConConexion(tramo1, tramo2, minutosEscala));
                    }
                }
            }
        }

        alternativas.sort(Comparator
                .comparing(ItinerarioVueloResponse::horarioSalida)
                .thenComparingInt(ItinerarioVueloResponse::cantidadEscalas));

        return alternativas;
    }

    private ItinerarioVueloResponse construirItinerarioDirecto(Vuelo vuelo) {
        TramoVueloResponse tramo = mapearTramo(vuelo);
        return ItinerarioVueloResponse.builder()
                .tipoItinerario("DIRECTO")
                .cantidadEscalas(0)
                .aeropuertoEscalaIata(null)
                .ciudadEscala(null)
                .tiempoEscalaMinutos(null)
                .horarioSalida(vuelo.getSalidaProgramada())
                .horarioLlegada(vuelo.getLlegadaProgramada())
                .duracionTotalMinutos(tramo.duracionMinutos())
                .tramos(List.of(tramo))
                .build();
    }

    private ItinerarioVueloResponse construirItinerarioConConexion(
            Vuelo tramo1,
            Vuelo tramo2,
            long minutosEscala
    ) {
        TramoVueloResponse dtoTramo1 = mapearTramo(tramo1);
        TramoVueloResponse dtoTramo2 = mapearTramo(tramo2);
        Aeropuerto escala = tramo1.getRuta().getDestino();

        long duracionTotal = Duration.between(
                tramo1.getSalidaProgramada(),
                tramo2.getLlegadaProgramada()
        ).toMinutes();

        return ItinerarioVueloResponse.builder()
                .tipoItinerario("CON_CONEXION")
                .cantidadEscalas(1)
                .aeropuertoEscalaIata(escala.getCodigoIata())
                .ciudadEscala(escala.getCiudad())
                .tiempoEscalaMinutos(minutosEscala)
                .horarioSalida(tramo1.getSalidaProgramada())
                .horarioLlegada(tramo2.getLlegadaProgramada())
                .duracionTotalMinutos(duracionTotal)
                .tramos(List.of(dtoTramo1, dtoTramo2))
                .build();
    }

    private TramoVueloResponse mapearTramo(Vuelo vuelo) {
        Aeropuerto origen = vuelo.getRuta().getOrigen();
        Aeropuerto destino = vuelo.getRuta().getDestino();
        long duracionMinutos = Duration.between(
                vuelo.getSalidaProgramada(),
                vuelo.getLlegadaProgramada()
        ).toMinutes();

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
                .duracionMinutos(duracionMinutos)
                .aeronaveModelo(vuelo.getAeronave().getModelo())
                .estadoVuelo(vuelo.getEstado().name())
                .build();
    }
}