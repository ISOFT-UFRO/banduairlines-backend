package com.banduairlines.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long>{
    @Query("""
    select distinct
        year(coalesce(p.fechaPago, p.createdAt)),
        month(coalesce(p.fechaPago, p.createdAt))
    from Pago p
    where coalesce(p.fechaPago, p.createdAt) < :inicioMesActual
    order by
        year(coalesce(p.fechaPago, p.createdAt)) desc,
        month(coalesce(p.fechaPago, p.createdAt)) desc
    """)
    List<Object[]> buscarMesesHistoricosReportes(
        @Param("inicioMesActual") LocalDateTime inicioMesActual);
}
