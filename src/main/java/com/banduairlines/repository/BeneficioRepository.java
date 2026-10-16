package com.banduairlines.repository;

import com.banduairlines.model.Beneficio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BeneficioRepository extends JpaRepository<Beneficio, Long> {

    List<Beneficio> findAllByOrderByCostoPuntosAscNombreAsc();
}