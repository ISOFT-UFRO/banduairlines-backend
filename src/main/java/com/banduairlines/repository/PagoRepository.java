package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Pago;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.dto.reportes.IngresoDiarioProjection;
import java.time.LocalDateTime;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long>{
    @Query(value = "select cast(p.fecha_pago as date) as fecha, p.moneda as moneda, " +
            "sum(p.monto) as ingresos, count(*) as pagos from pagos p " +
            "where p.estado = 'PAGADO' and p.fecha_pago >= :desde and p.fecha_pago < :hasta " +
            "group by cast(p.fecha_pago as date), p.moneda order by fecha, moneda", nativeQuery = true)
    List<IngresoDiarioProjection> obtenerIngresosDiarios(
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select sum(p.monto) from Pago p where p.estado = :estado and p.moneda = :moneda " +
            "and p.fechaPago >= :desde and p.fechaPago < :hasta")
    java.math.BigDecimal sumarIngresosReporte(@Param("estado") EstadoPago estado,
            @Param("moneda") String moneda, @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query(value = "select p.moneda as moneda, sum(p.monto) as ingresos from pagos p " +
            "where p.estado = 'PAGADO' and p.fecha_pago >= :desde and p.fecha_pago < :hasta " +
            "group by p.moneda order by p.moneda", nativeQuery = true)
    List<com.banduairlines.dto.reportes.IngresoPorMonedaProjection> sumarIngresosPorMoneda(
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(value = "select count(*) from pagos p where p.estado = 'PAGADO' " +
            "and p.fecha_pago >= :desde and p.fecha_pago < :hasta", nativeQuery = true)
    long contarPagosConfirmados(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
