package com.banduairlines.service.reserva;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.banduairlines.dto.reserva.ContactoResponse;
import com.banduairlines.dto.reserva.PasajeroResponse;
import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.dto.reserva.TotalResponse;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.BoletoExtra;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Promocion;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.TipoPasajero;

/**
 * Arma la vista de una reserva a partir de sus boletos y pasajeros
 * 
 * El itinerario sale de los boletos (un tramo por vuelo distinto) y la cantidad de pasajeros
 * de los pasajeros guardados (provisorios o reales), porque el modelo no guarda esos datos aparte
 */
@Component
public class ResumenReserva {

    private final CalculadoraTotalReserva calculadora;

    public ResumenReserva(CalculadoraTotalReserva calculadora) {
        this.calculadora = calculadora;
    }

    public ReservaResponse armar(Reserva reserva, List<Pasajero> pasajeros, List<Boleto> boletos) {
        boolean registrados = registrados(pasajeros);
        Promocion promocion = reserva.getPromocion();
        return new ReservaResponse(
                reserva.getId(),
                reserva.getCodigoReserva(),
                reserva.getEstado(),
                reserva.getExpiraEn() != null ? reserva.getExpiraEn().atOffset(ZoneOffset.UTC) : null,
                boletos.isEmpty() ? null : boletos.get(0).getTarifaVueloId().getPack().getNombre(),
                cantidad(pasajeros),
                itinerario(boletos),
                registrados,
                registrados ? pasajeros.stream().map(p -> pasajero(p, null)).toList() : List.of(),
                registrados ? asignaciones(boletos) : List.of(),
                servicios(boletos),
                promocion == null ? null : new ReservaResponse.Promocion(
                        promocion.getCodigo(),
                        promocion.getTipoDescuento().name(),
                        CalculadoraTotalReserva.clp(promocion.getValor()),
                        promocion.getVigenteHasta().atOffset(ZoneOffset.UTC)),
                contacto(reserva),
                total(reserva, boletos));
    }

    /** Total con los precios guardados en los boletos (lo que vio el pasajero al reservar */
    public TotalResponse total(Reserva reserva, List<Boleto> boletos) {
        return calculadora.calcular(boletos, reserva.getPromocion(), false).aResponse();
    }

    /** true cuando ya se hizo PUT /pasajeros (no queda ningun pasajero provisorio) */
    public static boolean registrados(List<Pasajero> pasajeros) {
        return !pasajeros.isEmpty() && pasajeros.stream().noneMatch(PasajerosPendientes::esPendiente);
    }

    public static ReservaResponse.CantidadPasajeros cantidad(List<Pasajero> pasajeros) {
        int adultos = 0;
        int ninos = 0;
        int bebes = 0;
        for (Pasajero p : pasajeros) {
            if (p.getTipo() == TipoPasajero.ADULTO) {
                adultos++;
            } else if (p.getTipo() == TipoPasajero.NINO) {
                ninos++;
            } else {
                bebes++;
            }
        }
        return new ReservaResponse.CantidadPasajeros(adultos, ninos, bebes);
    }

    /** El correo se guarda vacio al crear una compra de invitado
     * mientras no haya contacto se devuelve null
     */
    public static ContactoResponse contacto(Reserva reserva) {
        if (reserva.getEmailContacto() == null || reserva.getEmailContacto().isBlank()) {
            return null;
        }
        return new ContactoResponse(reserva.getEmailContacto(), reserva.getTelefonoContacto());
    }

    public static PasajeroResponse pasajero(Pasajero p, String ref) {
        return new PasajeroResponse(
                p.getId(),
                ref,
                p.getTipo(),
                p.getNombre(),
                p.getApellido(),
                p.getFechaNacimiento(),
                p.getTipoDocumento(),
                p.getNumeroDocumento(),
                p.getAdultoResponsable() != null ? p.getAdultoResponsable().getId() : null);
    }

    public static List<ReservaResponse.Tramo> itinerario(List<Boleto> boletos) {
        Map<Long, ReservaResponse.Tramo> tramos = new LinkedHashMap<>();
        for (Boleto b : boletos) {
            Vuelo v = b.getVueloId();
            tramos.computeIfAbsent(v.getId(), id -> new ReservaResponse.Tramo(
                    v.getId(),
                    b.getOrdenTramo(),
                    b.getSentido(),
                    v.getCodigoVuelo(),
                    v.getRuta() != null ? v.getRuta().getOrigen().getCodigoIata() : null,
                    v.getRuta() != null ? v.getRuta().getDestino().getCodigoIata() : null,
                    v.getSalidaProgramada(),
                    v.getLlegadaProgramada(),
                    b.getTarifaVueloId().getClaseCabina(),
                    v.getEstado()));
        }
        return List.copyOf(tramos.values());
    }

    public static List<ReservaResponse.Asignacion> asignaciones(List<Boleto> boletos) {
        return boletos.stream()
                .map(b -> new ReservaResponse.Asignacion(
                        b.getPasajeroId().getId(),
                        b.getVueloId().getId(),
                        b.getAsientoId() != null ? AsientosReserva.codigo(b.getAsientoId()) : null))
                .toList();
    }

    public static List<ReservaResponse.Servicio> servicios(List<Boleto> boletos) {
        List<ReservaResponse.Servicio> servicios = new ArrayList<>();
        for (Boleto b : boletos) {
            for (BoletoExtra be : b.getExtras()) {
                if (CalculadoraTotalReserva.esServicio(be.getExtra().getTipo())) {
                    servicios.add(new ReservaResponse.Servicio(
                            be.getExtra().getId(),
                            b.getPasajeroId().getId(),
                            b.getVueloId().getId(),
                            be.getExtra().getNombre(),
                            be.getCantidad(),
                            CalculadoraTotalReserva.clp(be.getPrecioUnitario()) * be.getCantidad()));
                }
            }
        }
        return servicios;
    }
}
