package com.banduairlines.service.impl;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.PasajeroResponse;
import com.banduairlines.dto.reserva.PasajerosRegistradosResponse;
import com.banduairlines.dto.reserva.RegistrarPasajerosRequest;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.reserva.AsientosReserva;
import com.banduairlines.service.reserva.CalculadoraTotalReserva;
import com.banduairlines.service.reserva.PasajerosPendientes;
import com.banduairlines.service.reserva.ReservaAcceso;
import com.banduairlines.service.reserva.ReservaPasajerosService;
import com.banduairlines.service.reserva.ResumenReserva;

@Service
public class ReservaPasajerosServiceImpl implements ReservaPasajerosService {

    static final String CANTIDAD_PASAJEROS_DISTINTA = "CANTIDAD_PASAJEROS_DISTINTA";
    static final String TIPO_NO_COINCIDE_EDAD = "TIPO_NO_COINCIDE_EDAD";
    static final String MENOR_SIN_ADULTO_RESPONSABLE = "MENOR_SIN_ADULTO_RESPONSABLE";
    static final String DATOS_INCOMPLETOS = "DATOS_INCOMPLETOS";

    private final ReservaAcceso reservaAcceso;
    private final ReservaRepository reservaRepository;
    private final PasajeroRepository pasajeroRepository;
    private final BoletoRepository boletoRepository;
    private final CalculadoraTotalReserva calculadora;

    public ReservaPasajerosServiceImpl(ReservaAcceso reservaAcceso, ReservaRepository reservaRepository,
                                       PasajeroRepository pasajeroRepository, BoletoRepository boletoRepository,
                                       CalculadoraTotalReserva calculadora) {
        this.reservaAcceso = reservaAcceso;
        this.reservaRepository = reservaRepository;
        this.pasajeroRepository = pasajeroRepository;
        this.boletoRepository = boletoRepository;
        this.calculadora = calculadora;
    }

    /**
     * Reemplaza los datos de los pasajeros (provisorios la primera vez) en los mismos registros, emparejados
     * por orden dentro de cada tipo: así los boletos ya creados siguen apuntando al pasajero correcto
     * 
     * se puede repetir mientras la compra este abierta. Si cambia el adulto responsable de un menor,
     * se libera el asiento del menor para que vuelva a elegirlo junto a su nuevo adulto
     */
    @Override
    @Transactional
    public PasajerosRegistradosResponse registrar(Long reservaId, String compraToken, RegistrarPasajerosRequest request) {
        Reserva reserva = reservaAcceso.obtenerAutorizada(reservaId, compraToken);
        reservaAcceso.exigirEnCurso(reserva);

        List<Boleto> boletos = boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reserva.getId());
        List<Pasajero> existentes = pasajeroRepository.findByReserva_IdOrderByIdAsc(reserva.getId());
        if (boletos.isEmpty() || existentes.isEmpty()) {
            throw new ReglaNegocioException("La reserva no tiene tramos ni pasajeros declarados").conCodigo(DATOS_INCOMPLETOS);
        }
        List<RegistrarPasajerosRequest.Pasajero> pedidos = request.pasajeros();

        Map<TipoPasajero, List<Pasajero>> existentesPorTipo = new EnumMap<>(TipoPasajero.class);
        existentes.forEach(p -> existentesPorTipo.computeIfAbsent(p.getTipo(), t -> new ArrayList<>()).add(p));

        exigirCantidades(existentesPorTipo, pedidos);
        exigirTiposSegunEdad(pedidos, fechaPrimerTramo(boletos));
        Map<String, String> responsablePorRef = exigirAdultosResponsables(pedidos);

        Map<Long, Long> responsableAnterior = new HashMap<>();
        existentes.forEach(p -> responsableAnterior.put(p.getId(),
                p.getAdultoResponsable() != null ? p.getAdultoResponsable().getId() : null));

        // Paso 1: documento temporal único, para que intercambiar documentos entre pasajeros no choque
        // con la restricción única (reserva_id, numero_documento) mientras se actualizan las filas.
        existentes.forEach(p -> p.setNumeroDocumento(PasajerosPendientes.PREFIJO_DOCUMENTO + "TMP-" + p.getId()));
        pasajeroRepository.saveAllAndFlush(existentes);

        // Paso 2: datos reales.
        Map<String, Pasajero> porRef = new LinkedHashMap<>();
        Map<TipoPasajero, Integer> usados = new EnumMap<>(TipoPasajero.class);
        for (RegistrarPasajerosRequest.Pasajero pedido : pedidos) {
            int indice = usados.merge(pedido.tipo(), 1, Integer::sum) - 1;
            Pasajero pasajero = existentesPorTipo.get(pedido.tipo()).get(indice);
            pasajero.setNombre(pedido.nombres().trim());
            pasajero.setApellido(pedido.apellidos().trim());
            pasajero.setFechaNacimiento(pedido.fechaNacimiento());
            pasajero.setTipoDocumento(pedido.tipoDocumento());
            pasajero.setNumeroDocumento(pedido.numeroDocumento().trim());
            pasajero.setAdultoResponsable(null);
            porRef.put(pedido.ref().trim(), pasajero);
        }
        responsablePorRef.forEach((refMenor, refAdulto) -> porRef.get(refMenor).setAdultoResponsable(porRef.get(refAdulto)));
        pasajeroRepository.saveAll(porRef.values());

        if (liberarAsientosSiCambioElResponsable(boletos, responsableAnterior)) {
            calculadora.aplicar(reserva, calculadora.calcular(boletos, reserva.getPromocion(), false));
            reservaRepository.save(reserva);
        }

        List<PasajeroResponse> respuesta = new ArrayList<>();
        porRef.forEach((ref, p) -> respuesta.add(ResumenReserva.pasajero(p, ref)));
        return new PasajerosRegistradosResponse(respuesta);
    }

    /** La cantidad y el tipo deben coincidir con lo declarado al crear la reserva. */
    private static void exigirCantidades(Map<TipoPasajero, List<Pasajero>> declarados,
                                         List<RegistrarPasajerosRequest.Pasajero> pedidos) {
        Map<TipoPasajero, Integer> enviados = new EnumMap<>(TipoPasajero.class);
        pedidos.forEach(p -> enviados.merge(p.tipo(), 1, Integer::sum));
        for (TipoPasajero tipo : TipoPasajero.values()) {
            int declarado = declarados.getOrDefault(tipo, List.of()).size();
            if (enviados.getOrDefault(tipo, 0) != declarado) {
                throw new ReglaNegocioException("La reserva es para " + cantidad(declarados, TipoPasajero.ADULTO)
                        + " adultos, " + cantidad(declarados, TipoPasajero.NINO) + " niños y "
                        + cantidad(declarados, TipoPasajero.BEBE) + " bebés; se enviaron "
                        + enviados.getOrDefault(TipoPasajero.ADULTO, 0) + ", "
                        + enviados.getOrDefault(TipoPasajero.NINO, 0) + " y "
                        + enviados.getOrDefault(TipoPasajero.BEBE, 0))
                        .conCodigo(CANTIDAD_PASAJEROS_DISTINTA);
            }
        }
    }

    private static int cantidad(Map<TipoPasajero, List<Pasajero>> porTipo, TipoPasajero tipo) {
        return porTipo.getOrDefault(tipo, List.of()).size();
    }

    /** ADULTO desde 12 años, NINO de 2 a 11 y BEBE menor de 2, a la fecha de salida del primer tramo. */
    private static void exigirTiposSegunEdad(List<RegistrarPasajerosRequest.Pasajero> pedidos, LocalDate fechaVuelo) {
        for (RegistrarPasajerosRequest.Pasajero p : pedidos) {
            TipoPasajero segunEdad = tipoSegunEdad(p.fechaNacimiento(), fechaVuelo);
            if (segunEdad != p.tipo()) {
                throw new ReglaNegocioException("El pasajero " + p.ref() + " se registró como " + p.tipo()
                        + ", pero a la fecha del vuelo (" + fechaVuelo + ") corresponde a " + segunEdad)
                        .conCodigo(TIPO_NO_COINCIDE_EDAD);
            }
        }
    }

    public static TipoPasajero tipoSegunEdad(LocalDate fechaNacimiento, LocalDate fechaVuelo) {
        int edad = Period.between(fechaNacimiento, fechaVuelo).getYears();
        if (edad >= 12) {
            return TipoPasajero.ADULTO;
        }
        return edad >= 2 ? TipoPasajero.NINO : TipoPasajero.BEBE;
    }

    /**
     * Todo bebé, y todo niño que viaja con adultos en la reserva, debe indicar un ADULTO de la misma petición.
     * Un adulto puede ser responsable de varios niños, pero de un solo bebé (va en sus brazos).
     * En un ADULTO, adultoResponsableRef se ignora. Devuelve ref del menor → ref de su adulto.
     */
    private static Map<String, String> exigirAdultosResponsables(List<RegistrarPasajerosRequest.Pasajero> pedidos) {
        Map<String, RegistrarPasajerosRequest.Pasajero> porRef = new HashMap<>();
        pedidos.forEach(p -> porRef.put(p.ref().trim(), p));
        boolean hayAdultos = pedidos.stream().anyMatch(p -> p.tipo() == TipoPasajero.ADULTO);

        Map<String, String> responsables = new LinkedHashMap<>();
        Map<String, Integer> bebesPorAdulto = new HashMap<>();
        for (RegistrarPasajerosRequest.Pasajero p : pedidos) {
            if (p.tipo() == TipoPasajero.ADULTO) {
                continue;
            }
            String refAdulto = p.adultoResponsableRef() == null || p.adultoResponsableRef().isBlank()
                    ? null : p.adultoResponsableRef().trim();
            boolean obligatorio = p.tipo() == TipoPasajero.BEBE || hayAdultos;
            if (refAdulto == null) {
                if (obligatorio) {
                    throw new ReglaNegocioException("El pasajero " + p.ref() + " (" + p.tipo()
                            + ") debe indicar su adulto responsable")
                            .conCodigo(MENOR_SIN_ADULTO_RESPONSABLE);
                }
                continue;
            }
            RegistrarPasajerosRequest.Pasajero adulto = porRef.get(refAdulto);
            if (adulto == null || adulto.tipo() != TipoPasajero.ADULTO) {
                throw new ReglaNegocioException("El adulto responsable " + refAdulto + " del pasajero " + p.ref()
                        + " no es un ADULTO de esta reserva")
                        .conCodigo(MENOR_SIN_ADULTO_RESPONSABLE);
            }
            if (p.tipo() == TipoPasajero.BEBE && bebesPorAdulto.merge(refAdulto, 1, Integer::sum) > 1) {
                throw new ReglaNegocioException("El adulto " + refAdulto + " ya es responsable de otro bebé; cada bebé"
                        + " viaja en brazos de un adulto distinto")
                        .conCodigo(MENOR_SIN_ADULTO_RESPONSABLE);
            }
            responsables.put(p.ref().trim(), refAdulto);
        }
        return responsables;
    }

    /** true si liberó algún asiento (cambia el total si era preferente). */
    private static boolean liberarAsientosSiCambioElResponsable(List<Boleto> boletos, Map<Long, Long> responsableAnterior) {
        boolean libero = false;
        for (Boleto boleto : boletos) {
            Pasajero p = boleto.getPasajeroId();
            Long actual = p.getAdultoResponsable() != null ? p.getAdultoResponsable().getId() : null;
            if (p.getTipo() != TipoPasajero.ADULTO && boleto.getAsientoId() != null
                    && !Objects.equals(actual, responsableAnterior.get(p.getId()))) {
                AsientosReserva.liberar(boleto);
                libero = true;
            }
        }
        return libero;
    }

    private static LocalDate fechaPrimerTramo(List<Boleto> boletos) {
        return boletos.stream()
                .map(b -> b.getVueloId().getSalidaProgramada())
                .min(Comparator.naturalOrder())
                .orElseThrow()
                .toLocalDate();
    }
}
