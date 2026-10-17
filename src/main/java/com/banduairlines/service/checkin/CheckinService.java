package com.banduairlines.service.checkin;

import com.banduairlines.dto.checkin.CheckinPasajeroDto;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.*;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.TarjetaEmbarqueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckinService {
    private final BoletoRepository boletoRepository;
    private final ReservaRepository reservaRepository;
    private final PasajeroRepository pasajeroRepository;
    private final TarjetaEmbarqueRepository tarjetaEmbarqueRepository;
    private final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public CheckinService(BoletoRepository boletoRepository, ReservaRepository reservaRepository, PasajeroRepository pasajeroRepository, TarjetaEmbarqueRepository tarjetaEmbarqueRepository) {
        this.boletoRepository = boletoRepository;
        this.reservaRepository = reservaRepository;
        this.pasajeroRepository = pasajeroRepository;
        this.tarjetaEmbarqueRepository = tarjetaEmbarqueRepository;
    }
    @Transactional
    public void realizarCheckin(CheckinPasajeroDto[] cpds,Reserva reserva,Vuelo vuelo){
        List<Pasajero> pasajeros = pasajeroRepository.findByReservaId(reserva.getId());
        List<Pasajero> pasajerosCheckedin = new ArrayList<>();
        for (CheckinPasajeroDto cpd : cpds) {
            Pasajero p = pasajeros.stream()
                    .filter((e)->e.getNombre().equals(cpd.nombres())&&e.getApellido().equals(cpd.apellidos()))
                    .findFirst().orElse(null);
            pasajerosCheckedin.add(p);
        }
        for (Pasajero p : pasajerosCheckedin) {
            List<Boleto> b = boletoRepository.findByPasajeroId(p);
            Boleto boleto = b.stream().filter((e)->e.getVueloId().equals(vuelo)).findFirst()
                    .orElseThrow(()-> new ReglaNegocioException("No existen boletos para este periodo"));
            String codigo = RANDOM.ints(12, 0, ALFABETO.length())
                    .mapToObj(ALFABETO::charAt)
                    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                    .toString();
            tarjetaEmbarqueRepository.save(new TarjetaEmbarque(0L,boleto,codigo,null,LocalDateTime.now()));
        }
    }

    public boolean verificarApellidos(Reserva reserva, String apellidos) {
        List<Pasajero> pasajeros = pasajeroRepository.findByReservaId(reserva.getId());
        return pasajeros.stream().filter((e)->e.getTipo().equals(TipoPasajero.ADULTO)).peek((e)-> System.out.println(e.getApellido()+","+e.getNombre())).anyMatch((e)->e.getApellido().equals(apellidos));
    }

    public Reserva obtenerReserva(String codigoReserva){
        List<Reserva> reservas = reservaRepository.findAll();
        System.out.println(reservas.getFirst().getCodigoReserva());
        return reservaRepository.findByCodigoReserva(codigoReserva).orElse(null);
    }

    @Transactional
    public Vuelo buscarVueloEnPeriodoCheckin(Reserva reserva) {
        List<Vuelo> vuelos = obtenerVuelosOrdenados(reserva);
        return vuelos.stream().filter((e)->LocalDateTime.now().isBefore(e.getSalidaProgramada().minusHours(1)))
                .filter((e)->LocalDateTime.now().isAfter(e.getSalidaProgramada().minusHours(48))).findFirst()
                .orElse(null);
    }
    public boolean calcularTramoTieneCheckin(Vuelo vuelo) {
        return !tarjetaEmbarqueRepository.findByBoletoVueloId(vuelo).isEmpty();
    }

    private List<Vuelo> obtenerVuelosOrdenados(Reserva reserva) {
        List<Boleto> boletos = boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reserva.getId());
        return boletos.stream()
                .map(Boleto::getVueloId)
                .collect(Collectors.toMap(
                        Vuelo::getId,
                        Function.identity(),
                        (a, b) -> a))
                .values().stream()
                .sorted(Comparator.comparing(Vuelo::getSalidaProgramada))
                .toList();
    }
}
