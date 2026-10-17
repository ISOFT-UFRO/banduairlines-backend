package com.banduairlines.integration;

import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Beneficio;
import com.banduairlines.model.CuentaFidelizacion;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.persistence.cuenta.RegistroPuntos;
import com.banduairlines.persistence.cuenta.TipoRegistroPuntos;
import com.banduairlines.repository.BeneficioRepository;
import com.banduairlines.repository.RegistroPuntosRepository;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioAutenticado;
import com.banduairlines.service.cuenta.CanjeService;
import com.banduairlines.service.cuenta.CanjeService.ResultadoCanje;
import com.banduairlines.service.cuenta.CodigosFidelizacion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba el canje contra PostgreSQL (igual que BanduAirlinesApplicationTests): necesita la base levantada
 * con docker compose up -d db. Crea sus propios datos y los borra al terminar.
 */
@SpringBootTest
class FlujoCanjeIntegracionTest {

    private static final long SALDO_INICIAL = 1000;
    private static final int COSTO_BENEFICIO = 600;

    @Autowired
    private CanjeService canjeService;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private BeneficioRepository beneficios;
    @Autowired
    private RegistroPuntosRepository registros;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private Clock reloj;

    private Long idUsuario;
    private Long idBeneficio;
    private String correo;

    @BeforeEach
    void crearDatos() {
        correo = "canje-" + UUID.randomUUID() + "@prueba.bandu.cl";
        Usuario usuario = Usuario.builder()
                .email(correo)
                .passwordHash("hash-de-prueba")
                .nombre("Ana")
                .apellido("Pérez")
                .rol(RolUsuario.PASAJERO)
                .activo(true)
                .build();
        usuario.setCuentaFidelizacion(CuentaFidelizacion.builder()
                .usuario(usuario)
                .saldoMillas(0)
                .nivel(Nivel.BASICO)
                .build());
        idUsuario = usuarios.save(usuario).getId();

        idBeneficio = beneficios.save(Beneficio.builder()
                .nombre("Sala VIP de prueba")
                .descripcion("Beneficio creado por FlujoCanjeIntegracionTest")
                .nivelRequerido(Nivel.BASICO)
                .costoPuntos(COSTO_BENEFICIO)
                .disponible(true)
                .build()).getIdBeneficio();

        registros.save(new RegistroPuntos(idUsuario, TipoRegistroPuntos.ACUMULACION, SALDO_INICIAL,
                reloj.instant(), "PRUEBA:" + UUID.randomUUID(), "ABC123"));
    }

    @AfterEach
    void borrarDatos() {
        SecurityContextHolder.clearContext();
        jdbc.update("delete from registros_puntos where usuario_id = ?", idUsuario);
        jdbc.update("delete from cuentas_fidelizacion where usuario_id = ?", idUsuario);
        jdbc.update("delete from usuarios where id = ?", idUsuario);
        jdbc.update("delete from beneficios where id_beneficio = ?", idBeneficio);
    }

    @Test
    void canjeCorrecto_descuentaPuntosYNoCambiaElNivel() {
        ResultadoCanje resultado = comoPasajero(() -> canjeService.canjearActual(idBeneficio, "clave-integracion-1"));

        assertTrue(resultado.creado());
        assertEquals(SALDO_INICIAL, resultado.canje().saldoAnterior());
        assertEquals(COSTO_BENEFICIO, resultado.canje().puntosDescontados());
        assertEquals(SALDO_INICIAL - COSTO_BENEFICIO, resultado.canje().saldoActual());
        assertEquals(-COSTO_BENEFICIO, resultado.canje().movimiento().cantidadPuntos());
        assertEquals(SALDO_INICIAL - COSTO_BENEFICIO, registros.calcularSaldo(idUsuario));
        assertEquals(Nivel.BASICO, nivelGuardado());
    }

    @Test
    void claveRepetida_noDescuentaDosVecesYDevuelveLaRespuestaOriginal() {
        ResultadoCanje primero = comoPasajero(() -> canjeService.canjearActual(idBeneficio, "clave-integracion-2"));
        // Entre el canje y el reintento el saldo cambia por una nueva acumulación.
        registros.save(new RegistroPuntos(idUsuario, TipoRegistroPuntos.ACUMULACION, 500,
                reloj.instant(), "PRUEBA:" + UUID.randomUUID(), "ABC123"));
        ResultadoCanje segundo = comoPasajero(() -> canjeService.canjearActual(idBeneficio, "clave-integracion-2"));

        assertTrue(primero.creado());
        assertFalse(segundo.creado());
        assertEquals(primero.canje().idCanje(), segundo.canje().idCanje());
        assertEquals(primero.canje().saldoAnterior(), segundo.canje().saldoAnterior());
        assertEquals(primero.canje().puntosDescontados(), segundo.canje().puntosDescontados());
        assertEquals(primero.canje().saldoActual(), segundo.canje().saldoActual());
        assertEquals(primero.canje().movimiento().cantidadPuntos(), segundo.canje().movimiento().cantidadPuntos());
        assertEquals(1, cantidadDeCanjes());
        assertEquals(SALDO_INICIAL - COSTO_BENEFICIO + 500, registros.calcularSaldo(idUsuario));
    }

    @Test
    void dosCanjesSimultaneos_soloUnoPasaYElSaldoNoQuedaNegativo() throws Exception {
        CountDownLatch partida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<ResultadoCanje> primero = hilos.submit(canjeEnOtroHilo(partida, "clave-simultanea-1"));
            Future<ResultadoCanje> segundo = hilos.submit(canjeEnOtroHilo(partida, "clave-simultanea-2"));
            partida.countDown();

            int exitosos = 0;
            int rechazadosPorPuntos = 0;
            for (Future<ResultadoCanje> canje : List.of(primero, segundo)) {
                try {
                    canje.get(30, TimeUnit.SECONDS);
                    exitosos++;
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof ReglaNegocioException regla
                            && CodigosFidelizacion.PUNTOS_INSUFICIENTES.equals(regla.getCodigo())) {
                        rechazadosPorPuntos++;
                    } else {
                        throw e;
                    }
                }
            }

            assertEquals(1, exitosos);
            assertEquals(1, rechazadosPorPuntos);
            assertEquals(1, cantidadDeCanjes());
            assertEquals(SALDO_INICIAL - COSTO_BENEFICIO, registros.calcularSaldo(idUsuario));
        } finally {
            hilos.shutdownNow();
        }
    }

    private Callable<ResultadoCanje> canjeEnOtroHilo(CountDownLatch partida, String clave) {
        return () -> {
            partida.await();
            return comoPasajero(() -> canjeService.canjearActual(idBeneficio, clave));
        };
    }

    /** Ejecuta la acción con el pasajero autenticado en el hilo actual. */
    private <T> T comoPasajero(Callable<T> accion) {
        var autenticacion = new UsernamePasswordAuthenticationToken(
                new UsuarioAutenticado(idUsuario, correo, RolUsuario.PASAJERO),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PASAJERO")));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
        try {
            return accion.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private int cantidadDeCanjes() {
        Integer cantidad = jdbc.queryForObject(
                "select count(*) from registros_puntos where usuario_id = ? and tipo = 'CANJE'",
                Integer.class, idUsuario);
        return cantidad == null ? 0 : cantidad;
    }

    private Nivel nivelGuardado() {
        String nivel = jdbc.queryForObject(
                "select nivel from cuentas_fidelizacion where usuario_id = ?", String.class, idUsuario);
        return Nivel.valueOf(nivel);
    }
}
