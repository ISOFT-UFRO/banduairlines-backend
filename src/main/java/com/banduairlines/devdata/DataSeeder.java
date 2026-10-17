package com.banduairlines.devdata;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.banduairlines.model.*;
import com.banduairlines.model.enums.*;
import com.banduairlines.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AeropuertoRepository aeropuertoRepository;
    private final RutaRepository rutaRepository;
    private final AvionRepository aeronaveRepository;
    private final AsientoRepository asientoRepository;
    private final PackTarifarioRepository packTarifarioRepository;
    private final VueloRepository vueloRepository;
    private final TarifaVueloRepository tarifaVueloRepository;
    private static final String PASSWORD_DEV = "Test1234!";

    private final UsuarioRepository usuarioRepository;
    private final CuentaFidelizacionRepository cuentaFidelizacionRepository;
    private final ExtraRepository extraRepository;
    private final PromocionRepository promocionRepository;
    private final ReservaRepository reservaRepository;
    private final PasajeroRepository pasajeroRepository;
    private final BoletoRepository boletoRepository;
    private final BoletoExtraRepository boletoExtraRepository;
    private final PagoRepository pagoRepository;
    private final MovimientoMillaRepository movimientoMillaRepository;
    private final SeguimientoVueloRepository seguimientoVueloRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public DataSeeder(AeropuertoRepository aeropuertoRepository,
                      RutaRepository rutaRepository,
                      AvionRepository aeronaveRepository,
                      AsientoRepository asientoRepository,
                      PackTarifarioRepository packTarifarioRepository,
                      VueloRepository vueloRepository,
                      TarifaVueloRepository tarifaVueloRepository, UsuarioRepository usuarioRepository, CuentaFidelizacionRepository cuentaFidelizacionRepository, ExtraRepository extraRepository, PromocionRepository promocionRepository, ReservaRepository reservaRepository, PasajeroRepository pasajeroRepository, BoletoRepository boletoRepository, BoletoExtraRepository boletoExtraRepository, PagoRepository pagoRepository, MovimientoMillaRepository movimientoMillaRepository, SeguimientoVueloRepository seguimientoVueloRepository, PasswordEncoder passwordEncoder) {
        this.aeropuertoRepository = aeropuertoRepository;
        this.rutaRepository = rutaRepository;
        this.aeronaveRepository = aeronaveRepository;
        this.asientoRepository = asientoRepository;
        this.packTarifarioRepository = packTarifarioRepository;
        this.vueloRepository = vueloRepository;
        this.tarifaVueloRepository = tarifaVueloRepository;
        this.usuarioRepository = usuarioRepository;
        this.cuentaFidelizacionRepository = cuentaFidelizacionRepository;
        this.extraRepository = extraRepository;
        this.promocionRepository = promocionRepository;
        this.reservaRepository = reservaRepository;
        this.pasajeroRepository = pasajeroRepository;
        this.boletoRepository = boletoRepository;
        this.boletoExtraRepository = boletoExtraRepository;
        this.pagoRepository = pagoRepository;
        this.movimientoMillaRepository = movimientoMillaRepository;
        this.seguimientoVueloRepository = seguimientoVueloRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (aeropuertoRepository.count() > 0) {
            log.info("DataSeeder: las tablas ya contienen datos. Se omite la carga inicial.");
            return;
        }

        log.warn("DATOS DE PRUEBA: solo para desarrollo local, no usar en servidor");
        log.info("DataSeeder: iniciando carga de datos iniciales...");

        // 1. Aeropuertos (~6 de Chile)
        Aeropuerto scl = aeropuertoRepository.save(Aeropuerto.builder()
                .codigoIata("SCL").nombre("Aeropuerto Internacional Arturo Merino Benítez")
                .ciudad("Santiago").pais("Chile").zonaHoraria("America/Santiago").build());

        Aeropuerto anf = aeropuertoRepository.save(Aeropuerto.builder()
                .codigoIata("ANF").nombre("Aeropuerto Andrés Sabella")
                .ciudad("Antofagasta").pais("Chile").zonaHoraria("America/Santiago").build());

        Aeropuerto cjc = aeropuertoRepository.save(Aeropuerto.builder()
                .codigoIata("CJC").nombre("Aeropuerto El Loa")
                .ciudad("Calama").pais("Chile").zonaHoraria("America/Santiago").build());

        Aeropuerto ccp = aeropuertoRepository.save(Aeropuerto.builder()
                .codigoIata("CCP").nombre("Aeropuerto Carriel Sur")
                .ciudad("Concepción").pais("Chile").zonaHoraria("America/Santiago").build());

        Aeropuerto pmc = aeropuertoRepository.save(Aeropuerto.builder()
                .codigoIata("PMC").nombre("Aeropuerto El Tepual")
                .ciudad("Puerto Montt").pais("Chile").zonaHoraria("America/Santiago").build());

        Aeropuerto puq = aeropuertoRepository.save(Aeropuerto.builder()
                .codigoIata("PUQ").nombre("Aeropuerto Presidente Carlos Ibáñez del Campo")
                .ciudad("Punta Arenas").pais("Chile").zonaHoraria("America/Punta_Arenas").build());

        // 2. Rutas en ambos sentidos
        List<Ruta> rutas = new ArrayList<>();
        rutas.add(crearRuta(scl, anf, 1100, 120));
        rutas.add(crearRuta(anf, scl, 1100, 120));

        rutas.add(crearRuta(scl, cjc, 1230, 125));
        rutas.add(crearRuta(cjc, scl, 1230, 125));

        rutas.add(crearRuta(scl, ccp, 430, 60));
        rutas.add(crearRuta(ccp, scl, 430, 60));

        rutas.add(crearRuta(scl, pmc, 915, 105));
        rutas.add(crearRuta(pmc, scl, 915, 105));

        rutas.add(crearRuta(scl, puq, 2190, 200));
        rutas.add(crearRuta(puq, scl, 2190, 200));

        rutas.add(crearRuta(pmc, puq, 1310, 130));
        rutas.add(crearRuta(puq, pmc, 1310, 130));
        rutas = rutaRepository.saveAll(rutas);

        // 3. Aviones (1-2) con asientos
        Avion avion1 = aeronaveRepository.save(Avion.builder()
                .matricula("CC-BAN").modelo("Airbus A320neo").estado(Estado.ACTIVA).build());

        Avion avion2 = aeronaveRepository.save(Avion.builder()
                .matricula("CC-BDU").modelo("Airbus A321neo").estado(Estado.ACTIVA).build());

        generarAsientos(avion1);
        generarAsientos(avion2);

        // 4. Packs tarifarios
        PackTarifario packLight = packTarifarioRepository.save(PackTarifario.builder()
                .nombre("Light").descripcion("Viaja ligero sin equipaje en bodega")
                .incluyeEquipajeCabina(true).piezasBodegaIncluidas(0).kgPorPiezaBodega(0)
                .incluyeSeleccionAsiento(false).incluyeEmbarquePrioritario(false).permiteCambios(false)
                .multiplicadorMillas(new BigDecimal("1.00")).activo(true).build());

        PackTarifario packPlus = packTarifarioRepository.save(PackTarifario.builder()
                .nombre("Plus").descripcion("Incluye 1 pieza de bodega y selección de asiento")
                .incluyeEquipajeCabina(true).piezasBodegaIncluidas(1).kgPorPiezaBodega(23)
                .incluyeSeleccionAsiento(true).incluyeEmbarquePrioritario(false).permiteCambios(true)
                .multiplicadorMillas(new BigDecimal("1.25")).activo(true).build());

        PackTarifario packTop = packTarifarioRepository.save(PackTarifario.builder()
                .nombre("Top").descripcion("Todo incluido: 2 piezas en bodega, embarque prioritario y cambios gratis")
                .incluyeEquipajeCabina(true).piezasBodegaIncluidas(2).kgPorPiezaBodega(23)
                .incluyeSeleccionAsiento(true).incluyeEmbarquePrioritario(true).permiteCambios(true)
                .multiplicadorMillas(new BigDecimal("1.50")).activo(true).build());

        // 5. Vuelos en los próximos 14 días con tarifas
        LocalDate hoy = LocalDate.now();
        List<Avion> aviones = List.of(avion1, avion2);

        int contadorVuelo = 100;
        for (int dia = 1; dia <= 14; dia++) {
            LocalDate fechaVuelo = hoy.plusDays(dia);

            for (int r = 0; r < rutas.size(); r++) {
                Ruta ruta = rutas.get(r);
                Avion avion = aviones.get((dia + r) % aviones.size());
                contadorVuelo++;

                LocalTime horaSalida = LocalTime.of(7 + (r * 2) % 14, (r * 15) % 60);
                LocalDateTime salida = LocalDateTime.of(fechaVuelo, horaSalida);
                LocalDateTime llegada = salida.plusMinutes(ruta.getDuracionMin());

                Vuelo vuelo = vueloRepository.save(Vuelo.builder()
                        .codigoVuelo("BA-" + contadorVuelo)
                        .ruta(ruta)
                        .aeronave(avion)
                        .salidaProgramada(salida)
                        .llegadaProgramada(llegada)
                        .estado(EstadoVuelo.PROGRAMADO)
                        .build());

                // Tarifas asociadas al vuelo
                tarifaVueloRepository.save(TarifaVuelo.builder()
                        .vuelo(vuelo).claseCabina(ClaseCabina.ECONOMY).pack(packLight)
                        .precio(new BigDecimal("29990.00")).build());

                tarifaVueloRepository.save(TarifaVuelo.builder()
                        .vuelo(vuelo).claseCabina(ClaseCabina.ECONOMY).pack(packPlus)
                        .precio(new BigDecimal("45990.00")).build());

                tarifaVueloRepository.save(TarifaVuelo.builder()
                        .vuelo(vuelo).claseCabina(ClaseCabina.ECONOMY).pack(packTop)
                        .precio(new BigDecimal("69990.00")).build());

                tarifaVueloRepository.save(TarifaVuelo.builder()
                        .vuelo(vuelo).claseCabina(ClaseCabina.PREMIUM_ECONOMY).pack(packPlus)
                        .precio(new BigDecimal("65990.00")).build());

                tarifaVueloRepository.save(TarifaVuelo.builder()
                        .vuelo(vuelo).claseCabina(ClaseCabina.PREMIUM_ECONOMY).pack(packTop)
                        .precio(new BigDecimal("89990.00")).build());

                tarifaVueloRepository.save(TarifaVuelo.builder()
                        .vuelo(vuelo).claseCabina(ClaseCabina.BUSINESS).pack(packTop)
                        .precio(new BigDecimal("139990.00")).build());
            }
        }
        sembrarReservas(hoy, packLight, packPlus, packTop);
        log.info("DataSeeder: carga inicial completada exitosamente.");
    }

    private Ruta crearRuta(Aeropuerto origen, Aeropuerto destino, int distanciaKm, int duracionMin) {
        return Ruta.builder()
                .origen(origen)
                .destino(destino)
                .distanciaKm(distanciaKm)
                .duracionMin(duracionMin)
                .activa(true)
                .build();
    }

    private void generarAsientos(Avion avion) {
        List<Asiento> asientos = new ArrayList<>();
        // Filas 1-2: BUSINESS (A, C, D, F)
        for (int fila = 1; fila <= 2; fila++) {
            for (String letra : List.of("A", "C", "D", "F")) {
                asientos.add(Asiento.builder()
                        .aeronave(avion).fila(fila).letra(letra)
                        .claseCabina(ClaseCabina.BUSINESS).esPreferente(true).build());
            }
        }
        // Filas 3-4: PREMIUM_ECONOMY (A, B, C, D, E, F)
        for (int fila = 3; fila <= 4; fila++) {
            for (String letra : List.of("A", "B", "C", "D", "E", "F")) {
                asientos.add(Asiento.builder()
                        .aeronave(avion).fila(fila).letra(letra)
                        .claseCabina(ClaseCabina.PREMIUM_ECONOMY).esPreferente(true).build());
            }
        }
        // Filas 5-15: ECONOMY (A, B, C, D, E, F)
        for (int fila = 5; fila <= 15; fila++) {
            boolean preferente = (fila == 10); // salida de emergencia
            for (String letra : List.of("A", "B", "C", "D", "E", "F")) {
                asientos.add(Asiento.builder()
                        .aeronave(avion).fila(fila).letra(letra)
                        .claseCabina(ClaseCabina.ECONOMY).esPreferente(preferente).build());
            }
        }
        asientoRepository.saveAll(asientos);
    }
    private void sembrarReservas(LocalDate hoy, PackTarifario packLight,
                                 PackTarifario packPlus, PackTarifario packTop) {
        LocalDateTime ahora = LocalDateTime.now();
        String passwordHash = passwordEncoder.encode(PASSWORD_DEV);

        List<Vuelo> vuelos = vueloRepository.findAll();
        List<TarifaVuelo> tarifas = tarifaVueloRepository.findAll();
        List<Asiento> asientos = asientoRepository.findAll();

        // ---- Usuarios ----
        Usuario admin = crearUsuario("admin@banduairlines.cl", passwordHash, RolUsuario.ADMINISTRADOR,
                "Admin", "Bandu", "11111111-1", LocalDate.of(1988, 1, 15), "+56911110000");
        Usuario maria = crearUsuario("maria.gonzalez@example.com", passwordHash, RolUsuario.PASAJERO,
                "María", "González", "22222222-2", LocalDate.of(1990, 5, 14), "+56922220000");
        Usuario juan = crearUsuario("juan.perez@example.com", passwordHash, RolUsuario.PASAJERO,
                "Juan", "Pérez", "33333333-3", LocalDate.of(1985, 11, 2), "+56933330000");
        Usuario camila = crearUsuario("camila.soto@example.com", passwordHash, RolUsuario.PASAJERO,
                "Camila", "Soto", "44444444-4", LocalDate.of(1998, 3, 27), "+56944440000");

        // ---- Extras ----
        Extra embarquePrioritario = crearExtra(TipoExtra.EMBARQUE_PRIORITARIO, "Embarque prioritario", null, "5990.00");
        Extra asientoPreferente = crearExtra(TipoExtra.ASIENTO_PREFERENTE, "Asiento preferente", null, "8990.00");
        Extra bodega23 = crearExtra(TipoExtra.EQUIPAJE_BODEGA, "Maleta de bodega 23 kg", 23, "24990.00");
        crearExtra(TipoExtra.EQUIPAJE_BODEGA, "Maleta de bodega 15 kg", 15, "19990.00");
        crearExtra(TipoExtra.EQUIPAJE_CABINA, "Maleta de cabina 10 kg", 10, "15990.00");

        // ---- Promociones (usos_actuales coincide con las reservas de prueba que las usan) ----
        Promocion bandu10 = crearPromocion("BANDU10", TipoDescuento.PORCENTAJE, "10.00",
                ahora.minusDays(30), ahora.plusDays(60), 1000, 1, true);
        Promocion familia15k = crearPromocion("FAMILIA15K", TipoDescuento.MONTO_FIJO, "15000.00",
                ahora.minusDays(30), ahora.plusDays(60), 500, 1, true);
        // Vencida y agotada, para probar validaciones
        crearPromocion("VERANO20", TipoDescuento.PORCENTAJE, "20.00",
                ahora.minusDays(120), ahora.minusDays(30), 200, 200, false);

        // =================================================================
        // RESERVA 1: CONFIRMADA, María, ida y vuelta SCL <-> CCP, Economy Plus,
        //            asientos elegidos, embarque prioritario en la ida, promo 10%
        // =================================================================
        Vuelo ida1 = buscarVuelo(vuelos, hoy, 3, "SCL", "CCP");
        Vuelo vuelta1 = buscarVuelo(vuelos, hoy, 6, "CCP", "SCL");
        TarifaVuelo tIda1 = buscarTarifa(tarifas, ida1, ClaseCabina.ECONOMY, packPlus);
        TarifaVuelo tVuelta1 = buscarTarifa(tarifas, vuelta1, ClaseCabina.ECONOMY, packPlus);

        BigDecimal subtotal1 = tIda1.getPrecio().add(tVuelta1.getPrecio())
                .add(embarquePrioritario.getPrecio());

        Reserva reserva1 = crearReserva("K7M3QX", maria, maria.getEmail(), maria.getTelefono(),
                EstadoReserva.CONFIRMADA, bandu10, subtotal1, null);

        Pasajero paxMaria = crearPasajero(reserva1, TipoPasajero.ADULTO, "María", "González",
                TipoDocumento.RUT, "22222222-2", LocalDate.of(1990, 5, 14), null);

        Boleto b1Ida = crearBoleto(reserva1, paxMaria, ida1, tIda1, Sentido.IDA, 1,
                buscarAsiento(asientos, ida1, 7, "A"), tIda1.getPrecio());
        Boleto b1Vuelta = crearBoleto(reserva1, paxMaria, vuelta1, tVuelta1, Sentido.VUELTA, 1,
                buscarAsiento(asientos, vuelta1, 7, "A"), tVuelta1.getPrecio());
        agregarExtra(b1Ida, embarquePrioritario, 1);

        crearPago(reserva1, EstadoPago.PAGADO, "FLOW-TKN-K7M3QX-0001", 1000001L, "Webpay",
                "{\"status\":2,\"paymentData\":{\"media\":\"Webpay\"}}", ahora.minusHours(2));

        int millasMaria = millasPorBoleto(tIda1, b1Ida.getPrecioTarifa())
                + millasPorBoleto(tVuelta1, b1Vuelta.getPrecioTarifa());

        // =================================================================
        // RESERVA 2: CONFIRMADA, familia de Juan (2 adultos, 1 niño, 1 bebé),
        //            SCL -> PMC solo ida, Economy Light, asientos preferentes
        //            (fila 10), 2 maletas de bodega, promo monto fijo
        // =================================================================
        Vuelo vuelo2 = buscarVuelo(vuelos, hoy, 5, "SCL", "PMC");
        TarifaVuelo tLight2 = buscarTarifa(tarifas, vuelo2, ClaseCabina.ECONOMY, packLight);

        BigDecimal subtotal2 = tLight2.getPrecio().multiply(BigDecimal.valueOf(3))              // 2 adultos + 1 niño
                .add(asientoPreferente.getPrecio().multiply(BigDecimal.valueOf(3)))              // 3 asientos fila 10
                .add(bodega23.getPrecio().multiply(BigDecimal.valueOf(2)));                      // 2 maletas

        Reserva reserva2 = crearReserva("R2T8LP", juan, juan.getEmail(), juan.getTelefono(),
                EstadoReserva.CONFIRMADA, familia15k, subtotal2, null);

        Pasajero paxJuan = crearPasajero(reserva2, TipoPasajero.ADULTO, "Juan", "Pérez",
                TipoDocumento.RUT, "33333333-3", LocalDate.of(1985, 11, 2), null);
        Pasajero paxPaola = crearPasajero(reserva2, TipoPasajero.ADULTO, "Paola", "Rojas",
                TipoDocumento.RUT, "55555555-5", LocalDate.of(1987, 8, 19), null);
        Pasajero paxMatias = crearPasajero(reserva2, TipoPasajero.NINO, "Matías", "Pérez",
                TipoDocumento.RUT, "25000000-6", hoy.minusYears(10), paxJuan);
        Pasajero paxSofia = crearPasajero(reserva2, TipoPasajero.BEBE, "Sofía", "Pérez",
                TipoDocumento.RUT, "26000000-4", hoy.minusMonths(10), paxPaola);

        Boleto b2Juan = crearBoleto(reserva2, paxJuan, vuelo2, tLight2, Sentido.IDA, 1,
                buscarAsiento(asientos, vuelo2, 10, "A"), tLight2.getPrecio());
        Boleto b2Paola = crearBoleto(reserva2, paxPaola, vuelo2, tLight2, Sentido.IDA, 1,
                buscarAsiento(asientos, vuelo2, 10, "B"), tLight2.getPrecio());
        Boleto b2Matias = crearBoleto(reserva2, paxMatias, vuelo2, tLight2, Sentido.IDA, 1,
                buscarAsiento(asientos, vuelo2, 10, "C"), tLight2.getPrecio());
        // Bebé en brazos: sin asiento y sin costo de tarifa
        Boleto b2Sofia = crearBoleto(reserva2, paxSofia, vuelo2, tLight2, Sentido.IDA, 1,
                null, BigDecimal.ZERO.setScale(2));

        agregarExtra(b2Juan, asientoPreferente, 1);
        agregarExtra(b2Juan, bodega23, 2);
        agregarExtra(b2Paola, asientoPreferente, 1);
        agregarExtra(b2Matias, asientoPreferente, 1);

        crearPago(reserva2, EstadoPago.PAGADO, "FLOW-TKN-R2T8LP-0002", 1000002L, "Webpay",
                "{\"status\":2,\"paymentData\":{\"media\":\"Webpay\"}}", ahora.minusDays(1));

        int millasJuan = millasPorBoleto(tLight2, b2Juan.getPrecioTarifa())
                + millasPorBoleto(tLight2, b2Paola.getPrecioTarifa())
                + millasPorBoleto(tLight2, b2Matias.getPrecioTarifa())
                + millasPorBoleto(tLight2, b2Sofia.getPrecioTarifa());

        // =================================================================
        // RESERVA 3: PENDIENTE_PAGO, invitado (sin usuario), SCL -> PUQ,
        //            Business Top, asiento 1A retenido, expira en 30 min
        // =================================================================
        Vuelo vuelo3 = buscarVuelo(vuelos, hoy, 8, "SCL", "PUQ");
        TarifaVuelo tTop3 = buscarTarifa(tarifas, vuelo3, ClaseCabina.BUSINESS, packTop);

        Reserva reserva3 = crearReserva("W9C4ZD", null, "pedro.fuentes@example.com", "+56955550000",
                EstadoReserva.PENDIENTE_PAGO, null, tTop3.getPrecio(), ahora.plusMinutes(30));

        Pasajero paxPedro = crearPasajero(reserva3, TipoPasajero.ADULTO, "Pedro", "Fuentes",
                TipoDocumento.PASAPORTE, "P1234567", LocalDate.of(1979, 1, 30), null);

        crearBoleto(reserva3, paxPedro, vuelo3, tTop3, Sentido.IDA, 1,
                buscarAsiento(asientos, vuelo3, 1, "A"), tTop3.getPrecio());

        crearPago(reserva3, EstadoPago.PENDIENTE, "FLOW-TKN-W9C4ZD-0003", 1000003L, null, null, null);

        // =================================================================
        // RESERVA 4: CANCELADA, Camila, SCL -> ANF, Economy Light,
        //            pago rechazado, sin asiento asignado
        // =================================================================
        Vuelo vuelo4 = buscarVuelo(vuelos, hoy, 2, "SCL", "ANF");
        TarifaVuelo tLight4 = buscarTarifa(tarifas, vuelo4, ClaseCabina.ECONOMY, packLight);

        Reserva reserva4 = crearReserva("H5N6VB", camila, camila.getEmail(), camila.getTelefono(),
                EstadoReserva.CANCELADA, null, tLight4.getPrecio(), null);

        Pasajero paxCamila = crearPasajero(reserva4, TipoPasajero.ADULTO, "Camila", "Soto",
                TipoDocumento.RUT, "44444444-4", LocalDate.of(1998, 3, 27), null);

        crearBoleto(reserva4, paxCamila, vuelo4, tLight4, Sentido.IDA, 1, null, tLight4.getPrecio());

        crearPago(reserva4, EstadoPago.RECHAZADO, "FLOW-TKN-H5N6VB-0004", 1000004L, "Webpay",
                "{\"status\":3,\"message\":\"Pago rechazado por el emisor\"}", null);

        // =================================================================
        // FIDELIZACIÓN: saldo = suma de movimientos; solo reservas CONFIRMADAS acumulan
        // =================================================================
        int bono = 500;

        CuentaFidelizacion cuentaMaria = crearCuentaFidelizacion(maria, bono + millasMaria, Nivel.PLATA);
        CuentaFidelizacion cuentaJuan = crearCuentaFidelizacion(juan, bono + millasJuan, Nivel.BASICO);
        CuentaFidelizacion cuentaCamila = crearCuentaFidelizacion(camila, bono, Nivel.BASICO);

        crearMovimiento(cuentaMaria, null, TipoMovimiento.AJUSTE, bono, "Bono de bienvenida");
        crearMovimiento(cuentaMaria, reserva1, TipoMovimiento.ACUMULACION, millasMaria,
                "Acumulación reserva " + reserva1.getCodigoReserva());

        crearMovimiento(cuentaJuan, null, TipoMovimiento.AJUSTE, bono, "Bono de bienvenida");
        crearMovimiento(cuentaJuan, reserva2, TipoMovimiento.ACUMULACION, millasJuan,
                "Acumulación reserva " + reserva2.getCodigoReserva());

        crearMovimiento(cuentaCamila, null, TipoMovimiento.AJUSTE, bono, "Bono de bienvenida");

        // =================================================================
        // SEGUIMIENTO DE VUELO (opcional): SCL -> ANF del día 1 se programó y luego se retrasó.
        // Para que vuelos.estado coincida, en tu loop de vuelos usa:
        //   .estado((dia == 1 && r == 0) ? EstadoVuelo.RETRASADO : EstadoVuelo.PROGRAMADO)
        // =================================================================
        Vuelo retrasado = buscarVuelo(vuelos, hoy, 1, "SCL", "ANF");
        crearSeguimiento(retrasado, EstadoVuelo.PROGRAMADO, null, "Vuelo programado", admin, ahora.minusDays(2));
        crearSeguimiento(retrasado, EstadoVuelo.RETRASADO, 45, "Retraso por congestión en pista", admin,
                ahora.minusHours(1));

        log.info("DataSeeder: usuarios de prueba creados (password: {}): admin@banduairlines.cl, "
                + "maria.gonzalez@example.com, juan.perez@example.com, camila.soto@example.com", PASSWORD_DEV);
    }

    // ---------------------------------------------------------------
    // CREACIÓN: reservas, pasajeros, boletos, cuentas
    // ---------------------------------------------------------------

    /** Crea una reserva; descuento y total se calculan a partir del subtotal y la promoción (puede ser null). */
    private Reserva crearReserva(String codigoReserva, Usuario usuario, String emailContacto,
                                 String telefonoContacto, EstadoReserva estado, Promocion promocion,
                                 BigDecimal subtotal, LocalDateTime expiraEn) {
        BigDecimal descuento = calcularDescuento(promocion, subtotal);
        return reservaRepository.save(Reserva.builder()
                .codigoReserva(codigoReserva).usuario(usuario)
                .emailContacto(emailContacto).telefonoContacto(telefonoContacto)
                .estado(estado).promocion(promocion)
                .subtotal(subtotal).descuento(descuento).total(subtotal.subtract(descuento))
                .moneda("CLP").expiraEn(expiraEn).build());
    }

    /** adultoResponsable: solo para NINO / BEBE; null para adultos. */
    private Pasajero crearPasajero(Reserva reserva, TipoPasajero tipo, String nombre, String apellido,
                                   TipoDocumento tipoDocumento, String numeroDocumento,
                                   LocalDate fechaNacimiento, Pasajero adultoResponsable) {
        return pasajeroRepository.save(Pasajero.builder()
                .reserva(reserva).tipo(tipo).nombre(nombre).apellido(apellido)
                .tipoDocumento(tipoDocumento).numeroDocumento(numeroDocumento)
                .fechaNacimiento(fechaNacimiento).adultoResponsable(adultoResponsable).build());
    }

    /** asiento puede ser null (bebé en brazos, o asiento aún sin elegir). */
    private Boleto crearBoleto(Reserva reserva, Pasajero pasajero, Vuelo vuelo, TarifaVuelo tarifa,
                               Sentido sentido, int ordenTramo, Asiento asiento, BigDecimal precioTarifa) {
        return boletoRepository.save(Boleto.builder()
                .reserva(reserva).pasajeroId(pasajero).vueloId(vuelo).tarifaVueloId(tarifa)
                .sentido(sentido).ordenTramo(ordenTramo).asientoId(asiento)
                .precioTarifa(precioTarifa).build());
    }

    private CuentaFidelizacion crearCuentaFidelizacion(Usuario usuario, int saldoMillas, Nivel nivel) {
        return cuentaFidelizacionRepository.save(CuentaFidelizacion.builder()
                .usuario(usuario).saldoMillas(saldoMillas).nivel(nivel).build());
    }

    // ---------------------------------------------------------------
    // CREACIÓN: datos de apoyo que faltaban (usuarios, extras, promociones,
    // extras por boleto, pagos, movimientos de millas, seguimiento de vuelo)
    // ---------------------------------------------------------------

    private Usuario crearUsuario(String email, String passwordHash, RolUsuario rol, String nombre, String apellido,
                                 String rut, LocalDate fechaNacimiento, String telefono) {
        return usuarioRepository.save(Usuario.builder()
                .email(email).passwordHash(passwordHash).rol(rol)
                .nombre(nombre).apellido(apellido)
                .tipoDocumento(TipoDocumento.RUT).numeroDocumento(rut)
                .fechaNacimiento(fechaNacimiento).telefono(telefono)
                .activo(true).build());
    }

    private Extra crearExtra(TipoExtra tipo, String nombre, Integer pesoKg, String precio) {
        return extraRepository.save(Extra.builder()
                .tipo(tipo).nombre(nombre).pesoKg(pesoKg)
                .precio(new BigDecimal(precio)).activo(true).build());
    }

    private Promocion crearPromocion(String codigo, TipoDescuento tipo, String valor,
                                     LocalDateTime vigenteDesde, LocalDateTime vigenteHasta,
                                     Integer usosMaximos, int usosActuales, boolean activa) {
        return promocionRepository.save(Promocion.builder()
                .codigo(codigo).tipoDescuento(tipo).valor(new BigDecimal(valor))
                .vigenteDesde(vigenteDesde).vigenteHasta(vigenteHasta)
                .usosMaximos(usosMaximos).usosActuales(usosActuales).activa(activa).build());
    }

    private void agregarExtra(Boleto boleto, Extra extra, int cantidad) {
        boletoExtraRepository.save(BoletoExtra.builder()
                .boleto(boleto).extra(extra).cantidad(cantidad)
                .precioUnitario(extra.getPrecio()).build());
    }

    private Pago crearPago(Reserva reserva, EstadoPago estado, String flowToken, Long flowOrder,
                           String medioPago, String detalleRespuestaJson, LocalDateTime fechaPago) {
        Pago pago = pagoRepository.save(Pago.builder()
                .reserva(reserva).monto(reserva.getTotal()).moneda("CLP").estado(estado)
                .flowToken(flowToken).flowOrder(flowOrder).medioPago(medioPago)
                .fechaPago(fechaPago).build());

        if (detalleRespuestaJson != null) {
            entityManager.createNativeQuery(
                            "UPDATE pagos SET detalle_respuesta = CAST(:detalle AS json) WHERE id = :id")
                    .setParameter("detalle", detalleRespuestaJson)
                    .setParameter("id", pago.getId())
                    .executeUpdate();
        }
        return pago;
    }

    private MovimientoMilla crearMovimiento(CuentaFidelizacion cuenta, Reserva reserva,
                                            TipoMovimiento tipo, int millas, String descripcion) {
        return movimientoMillaRepository.save(MovimientoMilla.builder()
                .cuentaFidelizacion(cuenta).reserva(reserva).tipo(tipo).millas(millas)
                .descripcion(descripcion).build());
    }

    private SeguimientoVuelo crearSeguimiento(Vuelo vuelo, EstadoVuelo estado, Integer retrasoMinutos,
                                              String observacion, Usuario registradoPor,
                                              LocalDateTime registradoEn) {
        return seguimientoVueloRepository.save(SeguimientoVuelo.builder()
                .vuelo(vuelo).estado(estado).retrasoMinutos(retrasoMinutos)
                .observacion(observacion).registradoPor(registradoPor)
                .registradoEn(registradoEn).build());
    }

    // ---------------------------------------------------------------
    // BÚSQUEDA de datos ya guardados por el resto del seeder
    // ---------------------------------------------------------------

    /** Vuelo que sale dentro de `dia` días (1..14) entre dos aeropuertos (códigos IATA). */
    private Vuelo buscarVuelo(List<Vuelo> vuelos, LocalDate hoy, int dia, String origenIata, String destinoIata) {
        LocalDate fecha = hoy.plusDays(dia);
        return vuelos.stream()
                .filter(v -> v.getSalidaProgramada().toLocalDate().equals(fecha))
                .filter(v -> v.getRuta().getOrigen().getCodigoIata().equals(origenIata))
                .filter(v -> v.getRuta().getDestino().getCodigoIata().equals(destinoIata))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay vuelo " + origenIata + "->" + destinoIata + " en hoy+" + dia));
    }

    private TarifaVuelo buscarTarifa(List<TarifaVuelo> tarifas, Vuelo vuelo, ClaseCabina clase, PackTarifario pack) {
        return tarifas.stream()
                .filter(t -> t.getVuelo().getCodigoVuelo().equals(vuelo.getCodigoVuelo()))
                .filter(t -> t.getClaseCabina() == clase)
                .filter(t -> t.getPack().getNombre().equals(pack.getNombre()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe tarifa " + clase + "/"
                        + pack.getNombre() + " para el vuelo " + vuelo.getCodigoVuelo()));
    }

    /** Asiento del avión asignado al vuelo (así nunca se mezcla con otra aeronave). */
    private Asiento buscarAsiento(List<Asiento> asientos, Vuelo vuelo, int fila, String letra) {
        return asientos.stream()
                .filter(a -> a.getAeronave().getMatricula().equals(vuelo.getAeronave().getMatricula()))
                .filter(a -> a.getFila() == fila && a.getLetra().equals(letra))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Asiento " + fila + letra + " no existe"));
    }

    // ---------------------------------------------------------------
    // CÁLCULOS
    // ---------------------------------------------------------------

    /** Millas = precio de la tarifa / 100 * multiplicador del pack (redondeo hacia abajo). */
    private int millasPorBoleto(TarifaVuelo tarifa, BigDecimal precio) {
        return precio.multiply(tarifa.getPack().getMultiplicadorMillas())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .intValue();
    }

    private BigDecimal calcularDescuento(Promocion promo, BigDecimal subtotal) {
        if (promo == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal descuento = promo.getTipoDescuento() == TipoDescuento.PORCENTAJE
                ? subtotal.multiply(promo.getValor()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : promo.getValor();
        return descuento.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }
}
