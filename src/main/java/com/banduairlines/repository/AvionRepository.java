package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.banduairlines.model.Avion;

import java.util.Optional;

public interface AvionRepository extends JpaRepository<Avion, Long> {
        Optional<Avion> findByMatricula(String matricula);
        boolean existsByMatricula(String matricula);
    }
