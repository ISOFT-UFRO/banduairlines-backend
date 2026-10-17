package com.banduairlines.service.checkin;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.checkin.PeticionReservaQuery;
import com.banduairlines.dto.checkin.PeticionReservaResponse;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Vuelo;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.ReservaRepository;

@Service 
public class ReservasService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    
    private ReservaRepository reservaRepository;
    private BoletoRepository boletoRepository;

    public ReservasService(ReservaRepository reservaRepository, BoletoRepository boletoRepository) {
        this.reservaRepository = reservaRepository;
        this.boletoRepository = boletoRepository;
    }

    @Transactional(readOnly = true)
    public Optional<PeticionReservaResponse> getReservaUsuarioNoRegistrado(PeticionReservaQuery dto){

        Optional<Reserva> reservaOpt = reservaRepository.findByCodigoReservaAndEmailContacto(dto.codigoPnr(),dto.correoElectronico());

        //
        Reserva reserva;
        if (reservaOpt.isEmpty()){
            return Optional.empty(); //Error 404
        } else {
            reserva = reservaOpt.get();
        }
        
        List<Vuelo> itinerario = getVuelosOrdenados(reserva);
        
        Vuelo primerVuelo = itinerario.get(0);
        Vuelo ultimoVuelo = itinerario.get(itinerario.size() - 1);
        String paisDestino = ultimoVuelo.getRuta().getDestino().getPais();
        String fechaSalida = primerVuelo.getSalidaProgramada().format(FECHA);
        String horaSalida  = primerVuelo.getSalidaProgramada().format(HORA);
        String nombre;
        String apellido;
        
        if (reserva.getUsuario() != null) {
            nombre = reserva.getUsuario().getNombre();
            apellido = reserva.getUsuario().getApellido();
        } else {
            // Reserva de invitado: no tiene usuario, se usa el pasajero del primer boleto (Compañero)
            Pasajero pasajero = boletoRepository.findByReserva(reserva).get(0).getPasajeroId();
            nombre = pasajero.getNombre();
            apellido = pasajero.getApellido();
        }

        return Optional.of(
            new PeticionReservaResponse(
                paisDestino,
                reserva.getCodigoReserva(),
                fechaSalida,
                horaSalida,
                null, // check_in (pendiente)
                nombre,
                apellido,
                reserva.getEmailContacto()
            )
        );
    }

    private List<Vuelo> getVuelosOrdenados(Reserva reserva) {
        List<Boleto> boletos = boletoRepository.findByReserva(reserva);

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
