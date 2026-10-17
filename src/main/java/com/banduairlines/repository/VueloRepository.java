package com.banduairlines.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Vuelo;

public interface VueloRepository extends JpaRepository<Vuelo, Long>{
    @Query("""
    select distinct year(v.salidaProgramada), month(v.salidaProgramada)
    from Vuelo v
    where v.salidaProgramada < :inicioMesActual
    order by year(v.salidaProgramada) desc, month(v.salidaProgramada) desc
    """)
    List<Object[]> buscarMesesHistoricosReportes(
        @Param("inicioMesActual") LocalDateTime inicioMesActual);
}
