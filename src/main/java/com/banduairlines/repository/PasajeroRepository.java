package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Pasajero;

import java.util.List;

public interface PasajeroRepository extends JpaRepository<Pasajero, Long>{
    List<Pasajero> findByReservaId(Long id);
}
