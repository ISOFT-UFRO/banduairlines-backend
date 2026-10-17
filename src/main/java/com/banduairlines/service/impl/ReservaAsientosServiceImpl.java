package com.banduairlines.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.MapaAsientosResponse;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Asiento;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Extra;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.CatalogoReservaRepository;
import com.banduairlines.service.reserva.AsientosReserva;
import com.banduairlines.service.reserva.ReservaAcceso;
import com.banduairlines.service.reserva.ReservaAsientosService;

@Service
public class ReservaAsientosServiceImpl implements ReservaAsientosService {

    static final String DISPONIBLE = "DISPONIBLE";
    static final String SELECCIONADO = "SELECCIONADO";
    static final String NO_DISPONIBLE = "NO_DISPONIBLE";

    private final ReservaAcceso reservaAcceso;
    private final BoletoRepository boletoRepository;
    private final CatalogoReservaRepository catalogoReservaRepository;
    private final AsientosReserva asientosReserva;

    public ReservaAsientosServiceImpl(ReservaAcceso reservaAcceso, BoletoRepository boletoRepository,
                                      CatalogoReservaRepository catalogoReservaRepository,
                                      AsientosReserva asientosReserva) {
        this.reservaAcceso = reservaAcceso;
        this.boletoRepository = boletoRepository;
        this.catalogoReservaRepository = catalogoReservaRepository;
        this.asientosReserva = asientosReserva;
    }

    /**
     * Vendidos o bloqueados por otra compra vigente, de otra cabina o sin precio configurado → NO_DISPONIBLE;
     * los de esta reserva → SELECCIONADO. precioCLP es lo que cuesta elegirlo con el pack de la reserva
     * 
     * tramoId es el id del vuelo, y debe ser uno de los tramos de la reserva (si no, 404)
     * 
     */

    @Override
    @Transactional(readOnly = true)
    public MapaAsientosResponse obtenerMapa(Long reservaId, Long tramoId, String compraToken) {
        Reserva reserva = reservaAcceso.obtenerAutorizada(reservaId, compraToken);
        List<Boleto> delTramo = boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reserva.getId()).stream()
                .filter(b -> b.getVueloId().getId().equals(tramoId))
                .toList();
        if (delTramo.isEmpty()) {
            throw new RecursoNoEncontradoException("Tramo", tramoId);
        }

        Boleto referencia = delTramo.get(0);
        Vuelo vuelo = referencia.getVueloId();
        ClaseCabina cabina = referencia.getTarifaVueloId().getClaseCabina();
        PackTarifario pack = referencia.getTarifaVueloId().getPack();
        Optional<Extra> preferente = asientosReserva.precioPreferente();
        List<Asiento> asientos = catalogoReservaRepository.asientosDeAeronave(vuelo.getAeronave().getId());

        Set<Long> tomados = new HashSet<>(boletoRepository.asientosTomadosPorOtras(
                vuelo.getId(), reserva.getId(), EstadoReserva.CONFIRMADA, ReservaAcceso.ABIERTAS, reservaAcceso.ahora()));
        Set<Long> propios = delTramo.stream()
                .filter(b -> b.getAsientoId() != null)
                .map(b -> b.getAsientoId().getId())
                .collect(Collectors.toSet());

        Map<Integer, List<MapaAsientosResponse.Asiento>> porFila = new LinkedHashMap<>();
        Map<Integer, ClaseCabina> cabinaPorFila = new HashMap<>();
        for (Asiento a : asientos) {
            Optional<Long> precio = AsientosReserva.precio(a, pack, preferente);
            String estado;
            if (propios.contains(a.getId())) {
                estado = SELECCIONADO;
            } else if (a.getClaseCabina() != cabina || precio.isEmpty() || tomados.contains(a.getId())) {
                estado = NO_DISPONIBLE;
            } else {
                estado = DISPONIBLE;
            }
            porFila.computeIfAbsent(a.getFila(), f -> new ArrayList<>()).add(new MapaAsientosResponse.Asiento(
                    AsientosReserva.codigo(a),
                    a.getLetra(),
                    AsientosReserva.categoria(a),
                    estado,
                    precio.orElse(0L),
                    AsientosReserva.incluidoEnPack(a, pack)));
            cabinaPorFila.putIfAbsent(a.getFila(), a.getClaseCabina());
        }

        List<String> columnas = AsientosReserva.columnas(asientos);
        List<MapaAsientosResponse.Fila> filas = porFila.entrySet().stream()
                .map(e -> new MapaAsientosResponse.Fila(e.getKey(), cabinaPorFila.get(e.getKey()), e.getValue()))
                .toList();
        return new MapaAsientosResponse(vuelo.getId(), cabina, columnas, AsientosReserva.pasillosDespuesDe(columnas), filas);
    }
}
