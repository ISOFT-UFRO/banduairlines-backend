package com.banduairlines.repository;

import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository
        extends JpaRepository<Reserva, Long> {

    // Utilizado por la consulta de detalle anterior.
    @Query("""
            SELECT COUNT(r)
            FROM Reserva r
            WHERE r.estado = :estado
              AND r.createdAt >= :desde
              AND r.createdAt < :hasta
            """)
    long contarReservasReporte(
            @Param("estado") EstadoReserva estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    interface ReservaEstadoProjection {

        EstadoReserva getEstado();

        Long getCantidad();
    }

    // Una consulta para todos los estados.
    @Query("""
            SELECT
                r.estado AS estado,
                COUNT(r) AS cantidad
            FROM Reserva r
            WHERE r.createdAt >= :desde
              AND r.createdAt < :hasta
            GROUP BY r.estado
            """)
    List<ReservaEstadoProjection> contarPorEstadoReporte(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}