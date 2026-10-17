package com.banduairlines.repository;

import com.banduairlines.model.Pago;
import com.banduairlines.model.enums.EstadoPago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    // Utilizado por la consulta de detalle anterior.
    @Query("""
            SELECT SUM(p.monto)
            FROM Pago p
            WHERE p.estado = :estado
              AND p.moneda = :moneda
              AND p.fechaPago >= :desde
              AND p.fechaPago < :hasta
            """)
    BigDecimal sumarIngresosReporte(
            @Param("estado") EstadoPago estado,
            @Param("moneda") String moneda,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    // Proyección: recibe únicamente las columnas del resultado.
    interface IngresoDiarioProjection {

        LocalDate getFecha();

        BigDecimal getIngresos();
    }

    // Una consulta para todos los días del período.
    @Query(
            value = """
                    SELECT
                        CAST(fecha_pago AS DATE) AS fecha,
                        SUM(monto) AS ingresos
                    FROM pagos
                    WHERE estado = 'PAGADO'
                      AND moneda = 'CLP'
                      AND fecha_pago >= :desde
                      AND fecha_pago < :hasta
                    GROUP BY CAST(fecha_pago AS DATE)
                    ORDER BY fecha
                    """,
            nativeQuery = true
    )
    List<IngresoDiarioProjection> obtenerIngresosDiarios(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}