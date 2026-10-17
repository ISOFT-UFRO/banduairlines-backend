package com.banduairlines.repository;

import com.banduairlines.model.Boleto;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoPasajero;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BoletoRepository
        extends JpaRepository<Boleto, Long> {

    @Query("""
            SELECT COUNT(b)
            FROM Boleto b
            WHERE b.reserva.estado = :estado
              AND b.asientoId IS NOT NULL
              AND b.createdAt >= :desde
              AND b.createdAt < :hasta
            """)
    long contarAsientosAsignadosReporte(
            @Param("estado") EstadoReserva estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    // Utilizado por la consulta de detalle anterior.
    @Query("""
            SELECT COUNT(DISTINCT b.pasajeroId.id)
            FROM Boleto b
            WHERE b.reserva.estado = :estado
              AND b.pasajeroId.tipo = :tipo
              AND b.createdAt >= :desde
              AND b.createdAt < :hasta
            """)
    long contarPasajerosReporte(
            @Param("estado") EstadoReserva estado,
            @Param("tipo") TipoPasajero tipo,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    interface PasajeroTipoProjection {

        TipoPasajero getTipo();

        Long getCantidad();
    }

    // Una consulta para adultos, niños y bebés.
    @Query("""
            SELECT
                b.pasajeroId.tipo AS tipo,
                COUNT(DISTINCT b.pasajeroId.id) AS cantidad
            FROM Boleto b
            WHERE b.reserva.estado = :estado
              AND b.createdAt >= :desde
              AND b.createdAt < :hasta
            GROUP BY b.pasajeroId.tipo
            """)
    List<PasajeroTipoProjection> contarPorTipoReporte(
            @Param("estado") EstadoReserva estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}