package com.banduairlines.service.checkin;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.checkin.TarjetaResponseDTO;
import com.banduairlines.model.Aeropuerto;
import com.banduairlines.model.Asiento;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TarjetaEmbarque;
import com.banduairlines.model.Vuelo;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.TarjetaEmbarqueRepository;

@Service
public class TarjetasService {

    // Minutos antes de la salida programada (IMPORTANTE)
    private static final int MINUTOS_SALA = 40;
    private static final int MINUTOS_CIERRE_EMBARQUE = 15;

    private final ReservaRepository reservaRepository;
    private final TarjetaEmbarqueRepository tarjetaEmbarqueRepository;

    public TarjetasService(ReservaRepository reservaRepository, TarjetaEmbarqueRepository tarjetaEmbarqueRepository) {
        this.reservaRepository = reservaRepository;
        this.tarjetaEmbarqueRepository = tarjetaEmbarqueRepository;
    }

    // Pasajero por tramo.
    @Transactional(readOnly = true)
    public List<TarjetaResponseDTO> getTarjetas(String codigoPnr){

        Optional<Reserva> reservaOpt = reservaRepository.findByCodigoReserva(codigoPnr);
        if(reservaOpt.isEmpty()){
            return List.of(); //Error 404
        }

        // Sortear por salida programada y por usuario
        return tarjetaEmbarqueRepository.findByBoletoReserva(reservaOpt.get()).stream()
            .sorted(Comparator
                .comparing((TarjetaEmbarque t) -> t.getBoleto().getPasajeroId().getId())
                .thenComparing(t -> t.getBoleto().getVueloId().getSalidaProgramada()))
            .map(this::formarTarjeta)
            .toList();
    }

    private TarjetaResponseDTO formarTarjeta(TarjetaEmbarque tarjeta){
        Boleto boleto = tarjeta.getBoleto();
        Vuelo vuelo = boleto.getVueloId();
        Aeropuerto origen = vuelo.getRuta().getOrigen();
        Aeropuerto destino = vuelo.getRuta().getDestino();
        Pasajero pasajero = boleto.getPasajeroId();
        Asiento asiento = boleto.getAsientoId();
        LocalDateTime salida = vuelo.getSalidaProgramada();

        return new TarjetaResponseDTO(
            boleto.getReserva().getCodigoReserva(),
            origen.getCiudad(),
            destino.getCiudad(),
            pasajero.getNombre(),
            pasajero.getApellido(),
            origen.getPais(),
            destino.getPais(),
            vuelo.getCodigoVuelo(),
            salida.toLocalDate(),
            salida.minusMinutes(MINUTOS_SALA).toLocalTime(),
            salida.minusMinutes(MINUTOS_CIERRE_EMBARQUE).toLocalTime(),
            tarjeta.getPuertaEmbarque(),
            asiento != null ? asiento.getFila() + asiento.getLetra() : null, // A15
            tarjeta.getCodigoEmbarque()
        );
    }

}
