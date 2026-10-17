package com.banduairlines.service.impl;

import com.banduairlines.dto.busqueda.DetalleCotizacionResponse;
import com.banduairlines.dto.busqueda.SeleccionCotizacionRequest;
import com.banduairlines.dto.busqueda.SeleccionTramoCotizacionRequest;
import com.banduairlines.dto.busqueda.SeleccionReservaResponse;
import com.banduairlines.dto.busqueda.TarifaItinerarioResponse;
import com.banduairlines.dto.busqueda.TarifasBusquedaRequest;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.repository.TarifaVueloRepository;
import com.banduairlines.service.busqueda.CotizacionService;
import com.banduairlines.service.busqueda.PasajerosBusquedaService;
import com.banduairlines.service.busqueda.TarifasBusquedaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CotizacionServiceImpl implements CotizacionService {

    private final TarifasBusquedaService tarifasBusquedaService;
    private final PasajerosBusquedaService pasajerosBusquedaService;
    private final TarifaVueloRepository tarifaVueloRepository;

    @Override
    public DetalleCotizacionResponse calcularCotizacion(SeleccionCotizacionRequest request) {
        var pasajeros = pasajerosBusquedaService.validarYResumir(request);
        List<SeleccionTramoCotizacionRequest> seleccion = request.getTramos() != null ? request.getTramos()
                : List.of(new SeleccionTramoCotizacionRequest(request.getVueloId(), request.getTarifaId(),
                        ClaseCabina.valueOf(request.getClaseCabina())));
        var consulta = new TarifasBusquedaRequest();
        consulta.setVueloIds(seleccion.stream().map(SeleccionTramoCotizacionRequest::vueloId).toList());
        consulta.setCantidadAdultos(pasajeros.cantidadAdultos());
        consulta.setCantidadNinos(pasajeros.cantidadNinos());
        consulta.setCantidadBebes(pasajeros.cantidadBebes());
        var disponibles = tarifasBusquedaService.consultar(consulta);
        for (var tramo : seleccion) {
            if (!tarifaVueloRepository.existsById(tramo.tarifaId())) {
                throw new RecursoNoEncontradoException("Tarifa", tramo.tarifaId());
            }
        }
        TarifaItinerarioResponse oferta = disponibles.tarifas().stream()
                .filter(t -> coincide(t, seleccion)).findFirst()
                .orElseThrow(() -> new ConflictoException(
                        "La combinación de vuelos, cabinas y tarifas seleccionada no está disponible")
                        .conCodigo("SELECCION_NO_DISPONIBLE"));

        DetalleCotizacionResponse response = new DetalleCotizacionResponse();
        if (seleccion.size() == 1) {
            var tramo = seleccion.getFirst();
            response.setVueloId(tramo.vueloId());
            response.setTarifaId(tramo.tarifaId());
            response.setClaseCabina(tramo.claseCabina().name());
        }
        BigDecimal unitario = oferta.precioPorPasajero();
        response.setSubtotalAdultos(unitario.multiply(BigDecimal.valueOf(pasajeros.cantidadAdultos())));
        response.setSubtotalNinos(unitario.multiply(BigDecimal.valueOf(pasajeros.cantidadNinos())));
        response.setSubtotalBebes(unitario.multiply(BigDecimal.valueOf(pasajeros.cantidadBebes())));
        response.setImpuestos(BigDecimal.ZERO);
        response.setPrecioTotal(oferta.precioTotalTarifas());
        response.setPrecioPorPasajero(unitario);
        response.setMoneda(oferta.moneda());
        response.setPasajeros(pasajeros);
        response.setPack(oferta.pack());
        response.setTramos(oferta.tramos());
        response.setSeleccionReserva(new SeleccionReservaResponse(
                oferta.tramos().stream().map(t -> new SeleccionReservaResponse.Tramo(t.vueloId(), t.claseCabina())).toList(),
                oferta.pack().nombre(), new SeleccionReservaResponse.CantidadPasajeros(
                        pasajeros.cantidadAdultos(), pasajeros.cantidadNinos(), pasajeros.cantidadBebes())));
        return response;
    }

    private boolean coincide(TarifaItinerarioResponse oferta, List<SeleccionTramoCotizacionRequest> seleccion) {
        if (oferta.tramos().size() != seleccion.size()) {
            return false;
        }
        for (int i = 0; i < seleccion.size(); i++) {
            var pedido = seleccion.get(i);
            var disponible = oferta.tramos().get(i);
            if (!pedido.vueloId().equals(disponible.vueloId())
                    || !pedido.tarifaId().equals(disponible.tarifaId())
                    || pedido.claseCabina() != disponible.claseCabina()) {
                return false;
            }
        }
        return true;
    }
}
