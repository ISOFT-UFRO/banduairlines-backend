package com.banduairlines.service.reportes;

import com.banduairlines.dto.reportes.EstadisticasResponse;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PagoRepository;
import com.banduairlines.repository.ReservaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EstadisticasServiceImpl
        implements EstadisticasService {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final BoletoRepository boletoRepository;

    public EstadisticasServiceImpl(
            PagoRepository pagoRepository,
            ReservaRepository reservaRepository,
            BoletoRepository boletoRepository
    ) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.boletoRepository = boletoRepository;
    }

    @Override
    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public EstadisticasResponse obtener(
            LocalDate desde,
            LocalDate hasta
    ) {
        LocalDateTime inicio = desde.atStartOfDay();

        LocalDateTime finExclusivo =
                hasta.plusDays(1).atStartOfDay();

        // Consulta 1: ingresos por día.
        List<EstadisticasResponse.IngresoDiario> ingresosPorDia =
                obtenerIngresosPorDia(
                        desde,
                        hasta,
                        inicio,
                        finExclusivo
                );

        BigDecimal totalIngresos = ingresosPorDia.stream()
                .map(EstadisticasResponse.IngresoDiario::ingresos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Consulta 2: reservas por estado.
        List<EstadisticasResponse.ReservasPorEstado> reservasPorEstado =
                obtenerReservasPorEstado(
                        inicio,
                        finExclusivo
                );

        long totalReservas = reservasPorEstado.stream()
                .mapToLong(
                        EstadisticasResponse.ReservasPorEstado::cantidad
                )
                .sum();

        long confirmadas = reservasPorEstado.stream()
                .filter(fila ->
                        fila.estado().equals(
                                EstadoReserva.CONFIRMADA.name()
                        )
                )
                .mapToLong(
                        EstadisticasResponse.ReservasPorEstado::cantidad
                )
                .sum();

        // Consulta 3: pasajeros por tipo.
        List<EstadisticasResponse.PasajerosPorTipo> pasajerosPorTipo =
                obtenerPasajerosPorTipo(
                        inicio,
                        finExclusivo
                );

        // Consulta 4: boletos con asiento asignado.
        long asientosAsignados = boletoRepository
                .contarAsientosAsignadosReporte(
                        EstadoReserva.CONFIRMADA,
                        inicio,
                        finExclusivo
                );

        return new EstadisticasResponse(
                desde,
                hasta,
                "CLP",
                totalIngresos,
                totalReservas,
                confirmadas,
                asientosAsignados,
                ingresosPorDia,
                reservasPorEstado,
                pasajerosPorTipo
        );
    }

    private List<EstadisticasResponse.IngresoDiario>
    obtenerIngresosPorDia(
            LocalDate desde,
            LocalDate hasta,
            LocalDateTime inicio,
            LocalDateTime finExclusivo
    ) {
        Map<LocalDate, BigDecimal> resultados = new HashMap<>();

        pagoRepository.obtenerIngresosDiarios(
                inicio,
                finExclusivo
        ).forEach(fila ->
                resultados.put(
                        fila.getFecha(),
                        fila.getIngresos()
                )
        );

        List<EstadisticasResponse.IngresoDiario> serie =
                new ArrayList<>();

        for (LocalDate fecha = desde;
             !fecha.isAfter(hasta);
             fecha = fecha.plusDays(1)) {

            serie.add(
                    new EstadisticasResponse.IngresoDiario(
                            fecha,
                            resultados.getOrDefault(
                                    fecha,
                                    BigDecimal.ZERO
                            )
                    )
            );
        }

        return List.copyOf(serie);
    }

    private List<EstadisticasResponse.ReservasPorEstado>
    obtenerReservasPorEstado(
            LocalDateTime inicio,
            LocalDateTime finExclusivo
    ) {
        Map<EstadoReserva, Long> resultados =
                new EnumMap<>(EstadoReserva.class);

        reservaRepository.contarPorEstadoReporte(
                inicio,
                finExclusivo
        ).forEach(fila ->
                resultados.put(
                        fila.getEstado(),
                        fila.getCantidad()
                )
        );

        List<EstadisticasResponse.ReservasPorEstado> serie =
                new ArrayList<>();

        for (EstadoReserva estado : EstadoReserva.values()) {

            serie.add(
                    new EstadisticasResponse.ReservasPorEstado(
                            estado.name(),
                            resultados.getOrDefault(estado, 0L)
                    )
            );
        }

        return List.copyOf(serie);
    }

    private List<EstadisticasResponse.PasajerosPorTipo>
    obtenerPasajerosPorTipo(
            LocalDateTime inicio,
            LocalDateTime finExclusivo
    ) {
        Map<TipoPasajero, Long> resultados =
                new EnumMap<>(TipoPasajero.class);

        boletoRepository.contarPorTipoReporte(
                EstadoReserva.CONFIRMADA,
                inicio,
                finExclusivo
        ).forEach(fila ->
                resultados.put(
                        fila.getTipo(),
                        fila.getCantidad()
                )
        );

        List<EstadisticasResponse.PasajerosPorTipo> serie =
                new ArrayList<>();

        for (TipoPasajero tipo : TipoPasajero.values()) {

            serie.add(
                    new EstadisticasResponse.PasajerosPorTipo(
                            tipo.name(),
                            resultados.getOrDefault(tipo, 0L)
                    )
            );
        }

        return List.copyOf(serie);
    }
}