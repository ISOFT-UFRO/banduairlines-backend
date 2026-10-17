package com.banduairlines.service.impl;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.CrearReservaRequest;
import com.banduairlines.dto.reserva.ReservaCreadaResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.Sentido;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.CatalogoReservaRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.security.UsuarioAutenticado;
import com.banduairlines.service.reserva.CalculadoraTotalReserva;
import com.banduairlines.service.reserva.CompraTokenService;
import com.banduairlines.service.reserva.PasajerosPendientes;
import com.banduairlines.service.reserva.ReservaAcceso;
import com.banduairlines.service.reserva.ReservaCreacionService;

@Service
public class ReservaCreacionServiceImpl implements ReservaCreacionService {

    static final String SIN_DISPONIBILIDAD = "SIN_DISPONIBILIDAD";
    static final String BEBES_SIN_ADULTO = "BEBES_SIN_ADULTO";

    /** Sin I, O, 0 ni 1 para que el codigo no se confunda al dictarlo o copiarlo. */
    private static final String ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LARGO_CODIGO = 6;
    private static final int INTENTOS_CODIGO = 10;

    /** Un vuelo ya embarcando, en vuelo, aterrizado o cancelado no se vende. */
    private static final Set<EstadoVuelo> VUELOS_VENDIBLES = Set.of(EstadoVuelo.PROGRAMADO, EstadoVuelo.RETRASADO);

    private final SecureRandom random = new SecureRandom();

    private final ReservaRepository reservaRepository;
    private final PasajeroRepository pasajeroRepository;
    private final BoletoRepository boletoRepository;
    private final CatalogoReservaRepository catalogoReservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioActual usuarioActual;
    private final CompraTokenService compraTokenService;
    private final ReservaAcceso reservaAcceso;
    private final PasajerosPendientes pasajerosPendientes;
    private final CalculadoraTotalReserva calculadora;
    private final long minutosBloqueo;

    public ReservaCreacionServiceImpl(ReservaRepository reservaRepository, PasajeroRepository pasajeroRepository,
                                      BoletoRepository boletoRepository, CatalogoReservaRepository catalogoReservaRepository,
                                      UsuarioRepository usuarioRepository, UsuarioActual usuarioActual,
                                      CompraTokenService compraTokenService, ReservaAcceso reservaAcceso,
                                      PasajerosPendientes pasajerosPendientes, CalculadoraTotalReserva calculadora,
                                      @Value("${app.reserva.minutos-bloqueo:15}") long minutosBloqueo) {
        this.reservaRepository = reservaRepository;
        this.pasajeroRepository = pasajeroRepository;
        this.boletoRepository = boletoRepository;
        this.catalogoReservaRepository = catalogoReservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioActual = usuarioActual;
        this.compraTokenService = compraTokenService;
        this.reservaAcceso = reservaAcceso;
        this.pasajerosPendientes = pasajerosPendientes;
        this.calculadora = calculadora;
        this.minutosBloqueo = minutosBloqueo;
    }

    /**
     * Valida primero lo que no depende de la base (bebes por adulto), luego vuelo, tarifa y cupo por tramo
     * 
     * solo si todo es valido se escribe: la reserva queda PENDIENTE_PAGO con un pasajero provisorio por cada
     * pasajero declarado y un boleto por pasajero y tramo, con el precio de la tarifa congelado
     */
    @Override
    @Transactional
    public ReservaCreadaResponse crear(CrearReservaRequest request) {
        CrearReservaRequest.CantidadPasajeros cantidad = request.cantidadPasajeros();
        if (cantidad.bebes() > cantidad.adultos()) {
            throw new ReglaNegocioException("Cada bebé debe viajar con un adulto: hay " + cantidad.bebes()
                    + " bebés y " + cantidad.adultos() + " adultos")
                    .conCodigo(BEBES_SIN_ADULTO);
        }

        LocalDateTime ahora = reservaAcceso.ahora();
        PackTarifario pack = buscarPack(request.pack());
        Map<Long, Vuelo> vuelos = bloquearVuelos(request.tramos());

        List<TarifaVuelo> tarifas = new ArrayList<>();
        for (CrearReservaRequest.Tramo pedido : request.tramos()) {
            Vuelo vuelo = vuelos.get(pedido.tramoId());
            exigirVueloVendible(vuelo, ahora);
            TarifaVuelo tarifa = catalogoReservaRepository
                    .buscarTarifa(vuelo.getId(), pedido.cabina(), pack.getId())
                    .orElseThrow(() -> new ConflictoException("El vuelo " + vuelo.getCodigoVuelo()
                            + " no ofrece la tarifa " + pack.getNombre() + " en cabina " + pedido.cabina())
                            .conCodigo(SIN_DISPONIBILIDAD));
            exigirCupo(vuelo, tarifa, cantidad.conAsiento(), ahora);
            tarifas.add(tarifa);
        }

        Usuario usuario = usuarioActual.obtener().map(UsuarioAutenticado::id).flatMap(usuarioRepository::findById).orElse(null);
        Reserva reserva = Reserva.builder()
                .codigoReserva(generarCodigo())
                .usuario(usuario)
                // El pasajero registrado parte con sus datos, el invitado los completa en PUT /contacto
                .emailContacto(usuario != null ? usuario.getEmail() : "")
                .telefonoContacto(usuario != null ? usuario.getTelefono() : null)
                .estado(EstadoReserva.PENDIENTE_PAGO)
                .expiraEn(ahora.plusMinutes(minutosBloqueo))
                .build();

        LocalDate fechaPrimerVuelo = vuelos.values().stream()
                .map(Vuelo::getSalidaProgramada)
                .min(Comparator.naturalOrder())
                .orElseThrow()
                .toLocalDate();
        List<Pasajero> pasajeros = crearPendientes(reserva, cantidad, fechaPrimerVuelo);

        List<Boleto> boletos = new ArrayList<>();
        for (int i = 0; i < request.tramos().size(); i++) {
            CrearReservaRequest.Tramo pedido = request.tramos().get(i);
            TarifaVuelo tarifa = tarifas.get(i);
            for (Pasajero pasajero : pasajeros) {
                boletos.add(Boleto.builder()
                        .reserva(reserva)
                        .pasajeroId(pasajero)
                        .vueloId(vuelos.get(pedido.tramoId()))
                        .tarifaVueloId(tarifa)
                        .sentido(pedido.sentido() != null ? pedido.sentido() : Sentido.IDA)
                        .ordenTramo(i + 1)
                        .precioTarifa(tarifa.getPrecio())
                        .build());
            }
        }

        CalculadoraTotalReserva.Resultado total = calculadora.calcular(boletos, null, false);
        reserva.setSubtotal(BigDecimal.valueOf(total.subtotal()));
        reserva.setDescuento(BigDecimal.valueOf(total.descuento()));
        reserva.setTotal(BigDecimal.valueOf(total.total()));

        reserva = reservaRepository.save(reserva);
        pasajeroRepository.saveAll(pasajeros);
        boletoRepository.saveAll(boletos);

        // Solo el invitado necesita credencial de compra; el pasajero registrado usa su JWT
        String compraToken = usuario == null ? compraTokenService.generar(reserva.getId()) : null;

        return new ReservaCreadaResponse(
                reserva.getId(),
                reserva.getCodigoReserva(),
                reserva.getEstado(),
                compraToken,
                reserva.getExpiraEn().atOffset(ZoneOffset.UTC),
                total.aResponse());
    }

    private List<Pasajero> crearPendientes(Reserva reserva, CrearReservaRequest.CantidadPasajeros cantidad, LocalDate fecha) {
        List<Pasajero> pasajeros = new ArrayList<>();
        int numero = 1;
        for (int i = 0; i < cantidad.adultos(); i++) {
            pasajeros.add(pasajerosPendientes.crear(reserva, TipoPasajero.ADULTO, numero++, fecha));
        }
        for (int i = 0; i < cantidad.ninos(); i++) {
            pasajeros.add(pasajerosPendientes.crear(reserva, TipoPasajero.NINO, numero++, fecha));
        }
        for (int i = 0; i < cantidad.bebes(); i++) {
            pasajeros.add(pasajerosPendientes.crear(reserva, TipoPasajero.BEBE, numero++, fecha));
        }
        return pasajeros;
    }

    /** Codigo de 6 caracteres, unico entre las reservas existentes */
    String generarCodigo() {
        for (int intento = 0; intento < INTENTOS_CODIGO; intento++) {
            StringBuilder codigo = new StringBuilder(LARGO_CODIGO);
            for (int i = 0; i < LARGO_CODIGO; i++) {
                codigo.append(ALFABETO_CODIGO.charAt(random.nextInt(ALFABETO_CODIGO.length())));
            }
            if (!reservaRepository.existsByCodigoReserva(codigo.toString())) {
                return codigo.toString();
            }
        }
        throw new IllegalStateException("No se pudo generar un código de reserva único");
    }

    private PackTarifario buscarPack(String nombre) {
        return catalogoReservaRepository.buscarPackPorNombre(nombre.trim())
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> new ConflictoException("La tarifa " + nombre.trim() + " no está disponible")
                        .conCodigo(SIN_DISPONIBILIDAD));
    }

    /**
     * Carga y bloquea los vuelos (en orden de id, para no generar interbloqueos) hasta el fin de la transacción:
     * asi dos compras simultaneas no pueden tomar el mismo ultimo cupo
     */
    private Map<Long, Vuelo> bloquearVuelos(List<CrearReservaRequest.Tramo> tramos) {
        Map<Long, Vuelo> vuelos = new HashMap<>();
        tramos.stream().map(CrearReservaRequest.Tramo::tramoId).sorted().forEach(id -> vuelos.put(id,
                catalogoReservaRepository.bloquearVuelo(id).orElseThrow(() -> new RecursoNoEncontradoException("Tramo", id))));
        return vuelos;
    }

    private void exigirVueloVendible(Vuelo vuelo, LocalDateTime ahora) {
        if (!VUELOS_VENDIBLES.contains(vuelo.getEstado()) || !vuelo.getSalidaProgramada().isAfter(ahora)) {
            throw new ConflictoException("El vuelo " + vuelo.getCodigoVuelo() + " ya no está a la venta")
                    .conCodigo(SIN_DISPONIBILIDAD);
        }
    }

    /** Asientos de la cabina en la aeronave menos las plazas que ya tienen otras compras vigentes */
    private void exigirCupo(Vuelo vuelo, TarifaVuelo tarifa, int plazasPedidas, LocalDateTime ahora) {
        if (plazasPedidas == 0) {
            return;
        }
        long capacidad = catalogoReservaRepository.contarAsientos(vuelo.getAeronave().getId(), tarifa.getClaseCabina());
        long tomadas = boletoRepository.plazasTomadas(vuelo.getId(), tarifa.getClaseCabina(), TipoPasajero.BEBE,
                EstadoReserva.CONFIRMADA, ReservaAcceso.ABIERTAS, ahora);
        if (tomadas + plazasPedidas > capacidad) {
            throw new ConflictoException("No quedan " + plazasPedidas + " asientos en cabina " + tarifa.getClaseCabina()
                    + " del vuelo " + vuelo.getCodigoVuelo())
                    .conCodigo(SIN_DISPONIBILIDAD);
        }
    }
}
