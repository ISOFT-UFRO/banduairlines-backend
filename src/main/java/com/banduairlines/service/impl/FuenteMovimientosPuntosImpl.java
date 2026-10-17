package com.banduairlines.service.impl;

import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.model.MovimientoMilla;
import com.banduairlines.model.enums.TipoMovimiento;
import com.banduairlines.repository.CuentaPuntosRepository;
import com.banduairlines.repository.MovimientoMillaRepository;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos;
import com.banduairlines.service.cuenta.RegistroPuntosService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class FuenteMovimientosPuntosImpl implements FuenteMovimientosPuntos {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entidades;
    private final MovimientoMillaRepository movimientos;
    private final CuentaPuntosRepository cuentas;
    private final JdbcTemplate jdbc;

    public FuenteMovimientosPuntosImpl(MovimientoMillaRepository movimientos,
                                      CuentaPuntosRepository cuentas, JdbcTemplate jdbc) {
        this.movimientos = movimientos;
        this.cuentas = cuentas;
        this.jdbc = jdbc;
    }

    @Override
    public Page<MovimientoPuntos> consultarMovimientos(Long idUsuario, Pageable pagina) {
        return movimientos.findByCuentaFidelizacion_UsuarioIdOrderByCreatedAtDescIdDesc(idUsuario, pagina)
                .map(this::convertir);
    }

    @Override
    public Map<Long, Long> contarCanjes(Collection<Long> idsBeneficio) {
        Map<Long, Long> cantidades = new HashMap<>();
        if (idsBeneficio.isEmpty()) return cantidades;
        for (var movimiento : movimientos.findByTipo(TipoMovimiento.CANJE)) {
            leerCanje(movimiento).ifPresent(datos -> {
                if (idsBeneficio.contains(datos.idBeneficio())) {
                    cantidades.merge(datos.idBeneficio(), 1L, Long::sum);
                }
            });
        }
        return cantidades;
    }

    @Override
    public Optional<CanjeRegistrado> buscarCanje(Long idUsuario, String claveIdempotencia) {
        return movimientos.findByCuentaFidelizacion_UsuarioIdAndTipo(idUsuario, TipoMovimiento.CANJE)
                .stream()
                .flatMap(m -> leerCanje(m)
                        .filter(datos -> datos.clave().equals(claveIdempotencia))
                        .map(datos -> convertirCanje(m, datos)).stream())
                .findFirst();
    }

    @Override
    @Transactional
    public ResultadoRegistroCanje registrarCanje(SolicitudRegistroCanje solicitud) {
        CanjeServiceImpl.validarClave(solicitud.claveIdempotencia());
        if (solicitud.idBeneficio() == null || solicitud.idBeneficio() <= 0
                || solicitud.nombreBeneficio() == null || solicitud.nombreBeneficio().isBlank()
                || solicitud.stockInicial() < 0)
            throw new ValidacionException("Los datos del beneficio son invalidos");
        var usuario = cuentas.bloquear(solicitud.idUsuario()).orElseThrow(RegistroPuntosService::cuentaNoEncontrada);
        RegistroPuntosService.exigirPasajero(usuario);
        jdbc.query("select pg_advisory_xact_lock(?)", (RowCallbackHandler) rs -> { }, 7314092601L);
        var anterior = buscarCanje(solicitud.idUsuario(), solicitud.claveIdempotencia());
        if (anterior.isPresent()) return new ResultadoRegistroCanje(anterior.get(), false);
        if (usuario.getCuentaFidelizacion() == null) throw new ReglaNegocioException("Falta la cuenta de fidelizacion");
        if (solicitud.costoPuntos() <= 0 || solicitud.costoPuntos() > Integer.MAX_VALUE)
            throw new ValidacionException("El costo del canje supera el rango permitido");
        long saldo = movimientos.calcularSaldo(solicitud.idUsuario());
        if (saldo < solicitud.costoPuntos())
            throw new ReglaNegocioException("No tienes puntos suficientes").conCodigo("PUNTOS_INSUFICIENTES");
        long usados = contarCanjes(java.util.Set.of(solicitud.idBeneficio())).getOrDefault(solicitud.idBeneficio(), 0L);
        if (usados >= solicitud.stockInicial())
            throw new ReglaNegocioException("El beneficio esta agotado").conCodigo("BENEFICIO_AGOTADO");
        String descripcion = "C1|" + solicitud.idBeneficio() + "|" + solicitud.claveIdempotencia()
                + "|" + saldo + "|" + solicitud.nombreBeneficio();
        if (descripcion.length() > 200) throw new ValidacionException("Los datos del canje superan el espacio disponible");
        var movimiento = movimientos.saveAndFlush(MovimientoMilla.builder()
                .cuentaFidelizacion(usuario.getCuentaFidelizacion()).tipo(TipoMovimiento.CANJE)
                .millas(-(int) solicitud.costoPuntos()).descripcion(descripcion).build());
        entidades.refresh(movimiento);
        return new ResultadoRegistroCanje(convertirCanje(movimiento), true);
    }

    private MovimientoPuntos convertir(MovimientoMilla m) {
        var datos = m.getTipo() == TipoMovimiento.CANJE ? leerCanje(m).orElse(null) : null;
        return new MovimientoPuntos(m.getId(), m.getTipo(), Math.abs(m.getMillas().longValue()),
                m.getCreatedAt().toInstant(ZoneOffset.UTC), datos == null ? m.getDescripcion() : "Canje de " + datos.nombre(),
                m.getReserva() == null ? null : m.getReserva().getCodigoReserva(),
                datos == null ? null : datos.idBeneficio());
    }

    private CanjeRegistrado convertirCanje(MovimientoMilla m) {
        var datos = leerCanje(m).orElseThrow();
        return convertirCanje(m, datos);
    }

    private CanjeRegistrado convertirCanje(MovimientoMilla m, DatosCanje datos) {
        return new CanjeRegistrado(convertir(m), datos.nombre(), datos.saldoAnterior(),
                Math.addExact(datos.saldoAnterior(), m.getMillas().longValue()));
    }

    private Optional<DatosCanje> leerCanje(MovimientoMilla m) {
        try {
            String[] partes = m.getDescripcion().split("\\|", 5);
            if (partes.length != 5 || !partes[0].equals("C1") || m.getMillas() >= 0)
                throw new IllegalArgumentException();
            return Optional.of(new DatosCanje(
                    Long.parseLong(partes[1]), partes[2], Long.parseLong(partes[3]), partes[4]));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private record DatosCanje(Long idBeneficio, String clave, long saldoAnterior, String nombre) { }
}
