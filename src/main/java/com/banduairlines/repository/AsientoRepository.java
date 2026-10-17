package com.banduairlines.repository;

import com.banduairlines.model.Asiento;
import com.banduairlines.model.enums.ClaseCabina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AsientoRepository extends JpaRepository<Asiento, Long> {

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