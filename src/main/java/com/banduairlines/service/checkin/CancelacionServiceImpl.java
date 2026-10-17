package com.banduairlines.service.checkin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.checkin.CancelacionInvitadoRequest;
import com.banduairlines.dto.checkin.CancelacionResponse;
import com.banduairlines.dto.checkin.CancelacionUsuarioRequest;
import com.banduairlines.dto.checkin.CondicionesCancelacionResponse;
import com.banduairlines.dto.checkin.CredencialesReservaRequest;
import com.banduairlines.dto.checkin.MotivoRestriccion;
import com.banduairlines.dto.checkin.ReembolsoResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Aeropuerto;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.BoletoExtra;
import com.banduairlines.model.Promocion;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.TipoDescuento;
import com.banduairlines.repository.CheckinCancelacionRepository;
import com.banduairlines.repository.PromocionRepository;
import com.banduairlines.security.UsuarioActual;

@Service
@Transactional
public class CancelacionServiceImpl implements CancelacionService {

    private static final Logger log = LoggerFactory.getLogger(CancelacionServiceImpl.class);

    private static final Duration ANTICIPACION_MINIMA = Duration.ofHours(1);

    private static final Set<String> ESTADOS_ANTES_DEL_VUELO = Set.of(
            EstadoVuelo.PROGRAMADO.name(), EstadoVuelo.RETRASADO.name(), EstadoVuelo.CANCELADO.name());

    /** Zona usada cuando un aeropuerto no tiene zona horaria y para las fechas del cupón. */
    private static final ZoneId ZONA_OPERACION = ZoneId.of("America/Santiago");

    private static final String PREFIJO_CUPON = "RB";
    private static final String ALFABETO_CUPON = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LARGO_SUFIJO_CUPON = 6;

    private final CheckinCancelacionRepository consultas;
    private final PromocionRepository promocionRepository;
    private final UsuarioActual usuarioActual;
    private final Clock clock;
    private final int vigenciaMesesCupon;
    private final boolean excepcionVueloCancelado;
    private final SecureRandom random = new SecureRandom();

    public CancelacionServiceImpl(CheckinCancelacionRepository consultas,
                                  PromocionRepository promocionRepository,
                                  UsuarioActual usuarioActual,
                                  Clock clock,
                                  @Value("${checkin.reembolso.vigencia-meses:12}") int vigenciaMesesCupon,
                                  @Value("${checkin.cancelacion.excepcion-vuelo-cancelado:true}") boolean excepcionVueloCancelado) {
        this.consultas = consultas;
        this.promocionRepository = promocionRepository;
        this.usuarioActual = usuarioActual;
        this.clock = clock;
        this.vigenciaMesesCupon = vigenciaMesesCupon;
        this.excepcionVueloCancelado = excepcionVueloCancelado;
    }

    @Override
    @Transactional(readOnly = true)
    public CondicionesCancelacionResponse consultarCondicionesInvitado(CredencialesReservaRequest request) {
        Reserva reserva = reservaDeInvitado(request.pnr(), request.apellidos(), false);
        return armarCondiciones(reserva, evaluar(reserva));
    }

    @Override
    @Transactional(readOnly = true)
    public CondicionesCancelacionResponse consultarCondicionesUsuario(Long idReserva) {
        Reserva reserva = reservaDeUsuario(idReserva, false);
        return armarCondiciones(reserva, evaluar(reserva));
    }

    @Override
    public CancelacionResponse cancelarInvitado(CancelacionInvitadoRequest request) {
        Reserva reserva = reservaDeInvitado(request.pnr(), request.apellidos(), true);
        return cancelar(reserva, request.motivoCancelacion());
    }

    @Override
    public CancelacionResponse cancelarUsuario(Long idReserva, CancelacionUsuarioRequest request) {
        Reserva reserva = reservaDeUsuario(idReserva, true);
        return cancelar(reserva, request != null ? request.motivoCancelacion() : null);
    }

    private CancelacionResponse cancelar(Reserva reserva, String motivoCancelacion) {
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ConflictoException("La reserva ya se encuentra cancelada")
                    .conCodigo("RESERVA_YA_CANCELADA");
        }
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new ReglaNegocioException("Solo se pueden cancelar reservas confirmadas")
                    .conCodigo("RESERVA_NO_CONFIRMADA");
        }

        Evaluacion evaluacion = evaluar(reserva);
        if (!evaluacion.cancelable()) {
            if (evaluacion.motivo() == MotivoRestriccion.VUELO_EN_CURSO) {
                throw new ReglaNegocioException("El vuelo ya inició su operación (embarque, despegue o aterrizaje); "
                        + "la reserva no se puede cancelar en línea")
                        .conCodigo("VUELO_EN_CURSO");
            }
            throw new ReglaNegocioException("La cancelación en línea cierra 1 hora antes de la salida del vuelo; "
                    + "debe realizarse de forma presencial en el aeropuerto")
                    .conCodigo("VENTANA_CANCELACION_CERRADA");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        liberarAsientos(evaluacion.boletos());
        consultas.eliminarTarjetasEmbarque(reserva.getId());

        log.info("Reserva {} cancelada. Motivo indicado por el pasajero: {}",
                reserva.getCodigoReserva(), motivoCancelacion != null ? motivoCancelacion : "(sin motivo)");

        return new CancelacionResponse(
                reserva.getCodigoReserva(),
                EstadoReserva.CANCELADA.name(),
                OffsetDateTime.now(clock),
                evaluacion.reembolsable(),
                aClp(evaluacion.montoReembolsable()));
    }

    private void liberarAsientos(List<Boleto> boletos) {
        boletos.forEach(boleto -> boleto.setAsientoId(null));
    }

    @Override
    public ReembolsoResponse solicitarReembolsoInvitado(CredencialesReservaRequest request) {
        Reserva reserva = reservaDeInvitado(request.pnr(), request.apellidos(), true);
        return solicitarReembolso(reserva);
    }

    @Override
    public ReembolsoResponse solicitarReembolsoUsuario(Long idReserva) {
        Reserva reserva = reservaDeUsuario(idReserva, true);
        return solicitarReembolso(reserva);
    }

    private ReembolsoResponse solicitarReembolso(Reserva reserva) {
        if (reserva.getEstado() != EstadoReserva.CANCELADA) {
            throw new ConflictoException("Para solicitar el reembolso primero se debe cancelar la reserva")
                    .conCodigo("RESERVA_NO_CANCELADA");
        }
        if (consultas.existeCuponConPrefijo(prefijoCupon(reserva))) {
            throw new ConflictoException("El reembolso de esta reserva ya fue emitido")
                    .conCodigo("REEMBOLSO_YA_EMITIDO");
        }

        Evaluacion evaluacion = evaluar(reserva);
        if (!evaluacion.reembolsable()) {
            throw new ReglaNegocioException("La tarifa contratada o el check-in realizado no permiten reembolso")
                    .conCodigo("REEMBOLSO_NO_CORRESPONDE");
        }

        Instant ahora = clock.instant();
        LocalDateTime desde = LocalDateTime.ofInstant(ahora, ZONA_OPERACION);

        Promocion cupon = promocionRepository.save(Promocion.builder()
                .codigo(prefijoCupon(reserva) + sufijoAleatorio())
                .tipoDescuento(TipoDescuento.MONTO_FIJO)
                .valor(evaluacion.montoReembolsable())
                .vigenteDesde(desde)
                .vigenteHasta(desde.plusMonths(vigenciaMesesCupon))
                .usosMaximos(1)
                .usosActuales(0)
                .activa(true)
                .build());

        long monto = aClp(evaluacion.montoReembolsable());
        log.info("Reembolso emitido para la reserva {}: cupón {} por {} CLP",
                reserva.getCodigoReserva(), cupon.getCodigo(), monto);

        return new ReembolsoResponse(
                cupon.getId(),
                reserva.getCodigoReserva(),
                "CUPON",
                "EMITIDO",
                monto,
                OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC),
                new ReembolsoResponse.Cupon(cupon.getCodigo(), monto,
                        cupon.getVigenteHasta().atZone(ZONA_OPERACION).toOffsetDateTime()));
    }

    private record Evaluacion(
            List<Boleto> boletos,
            Set<Long> boletosConCheckin,
            Map<Long, String> estadoPorVuelo,
            OffsetDateTime fechaLimite,
            boolean cancelable,
            boolean reembolsable,
            MotivoRestriccion motivo,
            BigDecimal montoPagado,
            BigDecimal montoReembolsable) {
    }

    private Evaluacion evaluar(Reserva reserva) {
        List<Boleto> boletos = consultas.buscarBoletosConDetalle(reserva.getId());
        if (boletos.isEmpty()) {
            throw new ReglaNegocioException("La reserva no tiene tramos asociados")
                    .conCodigo("RESERVA_SIN_TRAMOS");
        }
        Set<Long> conCheckin = consultas.buscarBoletosConCheckin(reserva.getId());
        Map<Long, String> estados = estadosDeTracking(boletos);

        OffsetDateTime primeraSalida = boletos.stream()
                .map(b -> salidaConZona(b.getVueloId()))
                .min(Comparator.naturalOrder())
                .orElseThrow();
        OffsetDateTime fechaLimite = primeraSalida.minus(ANTICIPACION_MINIMA);

        boolean confirmada = reserva.getEstado() == EstadoReserva.CONFIRMADA;
        boolean cancelada = reserva.getEstado() == EstadoReserva.CANCELADA;
        boolean dentroDeVentana = clock.instant().isBefore(fechaLimite.toInstant());

        boolean vueloIniciado = boletos.stream()
                .anyMatch(b -> !ESTADOS_ANTES_DEL_VUELO.contains(estadoDe(b, estados)));
        boolean hayVueloCancelado = boletos.stream()
                .anyMatch(b -> vueloCanceladoPorAerolinea(b, estados));

        boolean cancelable = confirmada && !vueloIniciado && (dentroDeVentana || hayVueloCancelado);

        BigDecimal montoPagado = Optional.ofNullable(consultas.sumarPagos(reserva.getId(), EstadoPago.PAGADO))
                .orElse(BigDecimal.ZERO);
        BigDecimal montoCalculado = calcularMontoReembolsable(boletos, conCheckin, estados, montoPagado);
        boolean cuponYaEmitido = cancelada && consultas.existeCuponConPrefijo(prefijoCupon(reserva));

        boolean reembolsable = montoCalculado.signum() > 0
                && !cuponYaEmitido
                && (cancelable || cancelada);
        BigDecimal montoReembolsable = reembolsable ? montoCalculado : BigDecimal.ZERO;

        MotivoRestriccion motivo = determinarMotivo(confirmada, dentroDeVentana, vueloIniciado,
                hayVueloCancelado, boletos, conCheckin, estados);

        return new Evaluacion(boletos, conCheckin, estados, fechaLimite, cancelable, reembolsable,
                motivo, montoPagado, montoReembolsable);
    }

    private BigDecimal calcularMontoReembolsable(List<Boleto> boletos, Set<Long> conCheckin,
                                                 Map<Long, String> estados, BigDecimal montoPagado) {
        BigDecimal totalBoletos = BigDecimal.ZERO;
        BigDecimal totalReembolsable = BigDecimal.ZERO;

        for (Boleto boleto : boletos) {
            BigDecimal valor = valorBoleto(boleto);
            totalBoletos = totalBoletos.add(valor);
            if (boletoReembolsable(boleto, conCheckin, estados)) {
                totalReembolsable = totalReembolsable.add(valor);
            }
        }

        if (totalBoletos.signum() == 0 || montoPagado.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return montoPagado.multiply(totalReembolsable).divide(totalBoletos, 0, RoundingMode.DOWN);
    }

    private boolean boletoReembolsable(Boleto boleto, Set<Long> conCheckin, Map<Long, String> estados) {
        if (vueloCanceladoPorAerolinea(boleto, estados)) {
            return true;
        }
        return tarifaReembolsable(boleto) && !conCheckin.contains(boleto.getId());
    }

    private BigDecimal valorBoleto(Boleto boleto) {
        BigDecimal valor = boleto.getPrecioTarifa();
        for (BoletoExtra extra : boleto.getExtras()) {
            valor = valor.add(extra.getPrecioUnitario().multiply(BigDecimal.valueOf(extra.getCantidad())));
        }
        return valor;
    }

    private boolean tarifaReembolsable(Boleto boleto) {
        return Boolean.TRUE.equals(boleto.getTarifaVueloId().getPack().getPermiteCambios());
    }

    private MotivoRestriccion determinarMotivo(boolean confirmada, boolean dentroDeVentana,
                                               boolean vueloIniciado, boolean hayVueloCancelado,
                                               List<Boleto> boletos, Set<Long> conCheckin,
                                               Map<Long, String> estados) {
        if (!confirmada) {
            return MotivoRestriccion.RESERVA_NO_CONFIRMADA;
        }
        if (vueloIniciado) {
            return MotivoRestriccion.VUELO_EN_CURSO;
        }
        if (!dentroDeVentana && !hayVueloCancelado) {
            return MotivoRestriccion.VENTANA_CERRADA;
        }
        // Los tramos cancelados por la aerolínea se reembolsan siempre; solo se revisan los demás.
        List<Boleto> sujetosATarifa = boletos.stream()
                .filter(b -> !vueloCanceladoPorAerolinea(b, estados))
                .toList();
        if (sujetosATarifa.stream().anyMatch(b -> conCheckin.contains(b.getId()))) {
            return MotivoRestriccion.CHECKIN_REALIZADO;   // CU-05 alternativa 2.b
        }
        if (sujetosATarifa.stream().anyMatch(b -> !tarifaReembolsable(b))) {
            return MotivoRestriccion.TARIFA_NO_REEMBOLSABLE;
        }
        return null;
    }

    private Reserva reservaDeInvitado(String pnr, String apellidos, boolean bloquear) {
        String codigo = pnr.trim().toUpperCase(Locale.ROOT);
        Optional<Reserva> encontrada = bloquear
                ? consultas.bloquearPorCodigo(codigo)
                : consultas.findByCodigoReserva(codigo);

        String apellidoBuscado = normalizar(apellidos);
        Reserva reserva = encontrada
                .filter(r -> consultas.buscarApellidosPasajero(r.getId()).stream()
                        .map(this::normalizar)
                        .anyMatch(apellidoBuscado::equals))
                .orElseThrow(this::reservaNoEncontrada);
        if (reserva.getUsuario() != null){
            throw new ReglaNegocioException("La reserva pertence a una cuenta registrada; inicia sesion para gestionarla")
                    .conCodigo("RESERVA_REQUIERE_SESION");
        }
        return reserva;
    }

    private Reserva reservaDeUsuario(Long idReserva, boolean bloquear) {
        Long usuarioId = usuarioActual.exigirId();   // 401 si no hay sesión
        Reserva reserva = (bloquear ? consultas.bloquearPorId(idReserva) : consultas.findById(idReserva))
                .orElseThrow(this::reservaNoEncontrada);

        if (reserva.getUsuario() == null || !usuarioId.equals(reserva.getUsuario().getId())) {
            throw reservaNoEncontrada();
        }
        return reserva;
    }

    private RecursoNoEncontradoException reservaNoEncontrada() {
        return new RecursoNoEncontradoException("No se encontró una reserva con los datos indicados")
                .conCodigo("RESERVA_NO_ENCONTRADA");
    }

    private Map<Long, String> estadosDeTracking(List<Boleto> boletos) {
        Set<Long> vueloIds = new HashSet<>();
        boletos.forEach(b -> vueloIds.add(b.getVueloId().getId()));

        Map<Long, String> estados = new HashMap<>();
        for (CheckinCancelacionRepository.EstadoVueloTramo fila : consultas.buscarEstadosVuelo(vueloIds)) {
            estados.put(fila.getVueloId(), fila.getEstadoActual());
        }
        return estados;
    }

    private String estadoDe(Boleto boleto, Map<Long, String> estados) {
        return estados.getOrDefault(boleto.getVueloId().getId(), EstadoVuelo.PROGRAMADO.name());
    }

    private boolean vueloCanceladoPorAerolinea(Boleto boleto, Map<Long, String> estados) {
        return excepcionVueloCancelado && EstadoVuelo.CANCELADO.name().equals(estadoDe(boleto, estados));
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private OffsetDateTime salidaConZona(Vuelo vuelo) {
        Aeropuerto origen = vuelo.getRuta().getOrigen();
        ZoneId zona = origen.getZonaHoraria() == null || origen.getZonaHoraria().isBlank()
                ? ZONA_OPERACION
                : ZoneId.of(origen.getZonaHoraria());
        return vuelo.getSalidaProgramada().atZone(zona).toOffsetDateTime();
    }

    private String prefijoCupon(Reserva reserva) {
        return PREFIJO_CUPON + reserva.getCodigoReserva();
    }

    private String sufijoAleatorio() {
        StringBuilder sb = new StringBuilder(LARGO_SUFIJO_CUPON);
        for (int i = 0; i < LARGO_SUFIJO_CUPON; i++) {
            sb.append(ALFABETO_CUPON.charAt(random.nextInt(ALFABETO_CUPON.length())));
        }
        return sb.toString();
    }

    private long aClp(BigDecimal monto) {
        return monto.setScale(0, RoundingMode.DOWN).longValueExact();
    }

    private CondicionesCancelacionResponse armarCondiciones(Reserva reserva, Evaluacion ev) {
        Map<String, CondicionesCancelacionResponse.Tramo> tramos = new LinkedHashMap<>();
        Map<Long, CondicionesCancelacionResponse.Pasajero> pasajeros = new LinkedHashMap<>();

        for (Boleto b : ev.boletos()) {
            Vuelo vuelo = b.getVueloId();
            String clave = vuelo.getId() + "-" + b.getTarifaVueloId().getId();
            tramos.putIfAbsent(clave, new CondicionesCancelacionResponse.Tramo(
                    vuelo.getCodigoVuelo(),
                    vuelo.getRuta().getOrigen().getCodigoIata(),
                    vuelo.getRuta().getDestino().getCodigoIata(),
                    salidaConZona(vuelo),
                    estadoDe(b, ev.estadoPorVuelo()),
                    b.getTarifaVueloId().getClaseCabina().name(),
                    b.getTarifaVueloId().getPack().getNombre(),
                    tarifaReembolsable(b)));

            boolean hizoCheckin = ev.boletosConCheckin().contains(b.getId());
            pasajeros.merge(b.getPasajeroId().getId(),
                    new CondicionesCancelacionResponse.Pasajero(
                            b.getPasajeroId().getNombre(),
                            b.getPasajeroId().getApellido(),
                            b.getPasajeroId().getTipo().name(),
                            hizoCheckin),
                    (anterior, nuevo) -> anterior.checkin() ? anterior : nuevo);
        }
        return new CondicionesCancelacionResponse(
                reserva.getCodigoReserva(),
                reserva.getEstado().name(),
                ev.cancelable(),
                ev.reembolsable(),
                ev.motivo(),
                ev.fechaLimite(),
                aClp(ev.montoPagado()),
                aClp(ev.montoReembolsable()),
                List.copyOf(tramos.values()),
                List.copyOf(pasajeros.values()));
    }
}