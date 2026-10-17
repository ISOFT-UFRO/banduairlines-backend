package com.banduairlines.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Pasajero;

public interface PasajeroRepository extends JpaRepository<Pasajero, Long>{

    /** RESERVA: pasajeros de una reserva en el orden en que se crearon. */
    List<Pasajero> findByReserva_IdOrderByIdAsc(Long reservaId);

    long countByReserva_Id(Long reservaId);
}
