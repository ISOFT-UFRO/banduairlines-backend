package com.banduairlines.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Asiento;
import com.banduairlines.model.enums.ClaseCabina;

public interface AsientoRepository extends JpaRepository<Asiento, Long> {

    List<Asiento> findByAeronaveId(Long aeronaveId);

    List<Asiento> findByAeronaveIdAndClaseCabina(Long aeronaveId, ClaseCabina claseCabina);

    @Query("""
            SELECT COUNT(a) FROM Asiento a
            WHERE a.aeronave.id = :aeronaveId
              AND a.claseCabina = :claseCabina
            """)
    long contarPorAeronaveYClaseCabina(
            @Param("aeronaveId") Long aeronaveId,
            @Param("claseCabina") ClaseCabina claseCabina
    );
}