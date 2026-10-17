package com.banduairlines.repository;

import com.banduairlines.model.enums.ClaseCabina;
import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Asiento;
import java.util.List;

public interface AsientoRepository extends JpaRepository<Asiento, Long>{
        List<Asiento> findByAeronaveId(Long aeronaveId);
        List<Asiento> findByAeronaveIdAndClaseCabina(Long aeronaveId, ClaseCabina claseCabina);
    }

