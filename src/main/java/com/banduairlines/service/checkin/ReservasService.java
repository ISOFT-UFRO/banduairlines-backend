package com.banduairlines.service.checkin;

import java.time.LocalDateTime;
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
import com.banduairlines.model.Usuario;
import com.banduairlines.model.Vuelo;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.TarjetaEmbarqueRepository;
import com.banduairlines.repository.UsuarioRepository;

@Service
public class ReservasService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    // Valores de checkin en la respuesta
    private static final String CHECKIN_REALIZADO = "REALIZADO";
    private static final String CHECKIN_DISPONIBLE = "DISPONIBLE";
    private static final String CHECKIN_NO_DISPONIBLE = "NO_DISPONIBLE";

    private ReservaRepository reservaRepository;
    private BoletoRepository boletoRepository;
    private UsuarioRepository usuarioRepository;
    private TarjetaEmbarqueRepository tarjetaEmbarqueRepository;

    public ReservasService(ReservaRepository reservaRepository, BoletoRepository boletoRepository, UsuarioRepository usuarioRepository, TarjetaEmbarqueRepository tarjetaEmbarqueRepository) {
        this.reservaRepository = reservaRepository;
        this.boletoRepository = boletoRepository;
        this.usuarioRepository = usuarioRepository;
        this.tarjetaEmbarqueRepository = tarjetaEmbarqueRepository;
    }

    // USUARIO REGISTRADO
    @Transactional(readOnly = true)
    public Optional<List<PeticionReservaResponse>> getReservasUsuarioRegistrado(Long id){

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()){
            return Optional.empty(); //Error 404
        }

        List<PeticionReservaResponse> reservas = reservaRepository.findByUsuarioId(id).stream()
            .map(this::toResponse)
            .flatMap(Optional::stream)
            .sorted(Comparator.comparing(PeticionReservaResponse::fechaSalida)
                .thenComparing(PeticionReservaResponse::horaSalida))
            .toList();

        return Optional.of(reservas);
    }


    // USUARIO NO REGISTRADO
    @Transactional(readOnly = true)
    public Optional<PeticionReservaResponse> getReservaUsuarioNoRegistrado(PeticionReservaQuery dto){
        
        return reservaRepository
            .findByCodigoReservaAndEmailContacto(dto.codigoPnr(), dto.correoElectronico())
            .flatMap(this::toResponse); //Puede devolver Optional<null>
    }

    // Devuelve vacio si la reserva no tiene boletos (sin vuelos no hay fecha ni destino)
    private Optional<PeticionReservaResponse> toResponse(Reserva reserva) {
        List<Boleto> boletos = boletoRepository.findByReserva(reserva);
        if (boletos.isEmpty()) {
            return Optional.empty();
        }

        List<Vuelo> itinerario = getVuelosOrdenados(boletos);

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
            Pasajero pasajero = boletos.get(0).getPasajeroId();
            nombre = pasajero.getNombre();
            apellido = pasajero.getApellido();
        }

        return Optional.of(
            new PeticionReservaResponse(
                paisDestino,
                reserva.getCodigoReserva(),
                fechaSalida,
                horaSalida,
                calcularEstadoCheckin(reserva),
                nombre,
                apellido,
                reserva.getEmailContacto()
            )
        );
    }

    private String calcularEstadoCheckin(Reserva reserva) {
        Vuelo vuelo = buscarVueloEnPeriodoCheckin(reserva);
        if (vuelo == null) {
            return CHECKIN_NO_DISPONIBLE;
        }
        return calcularTramoTieneCheckin(vuelo) ? CHECKIN_REALIZADO : CHECKIN_DISPONIBLE;
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
        return getVuelosOrdenados(boletoRepository.findByReserva(reserva));
    }

    private List<Vuelo> getVuelosOrdenados(List<Boleto> boletos) {
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
