package com.banduairlines.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long>{
    @Query("""
    select distinct year(r.createdAt), month(r.createdAt)
    from Reserva r
    where r.createdAt < :inicioMesActual
    order by year(r.createdAt) desc, month(r.createdAt) desc
    """)
    List<Object[]> buscarMesesHistoricosReportes(
        @Param("inicioMesActual") LocalDateTime inicioMesActual);
}
