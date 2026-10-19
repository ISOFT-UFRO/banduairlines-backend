package com.banduairlines.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;

// Lectura de Reserva propia de Tracking (RF-04), para no modificar ReservaRepository (Reserva).
public interface TrackingReservaLecturaRepository extends Repository<Reserva, Long> {

    @Query("select r.id from Reserva r where r.usuario.id = :usuarioId and r.estado = :estado")
    List<Long> buscarIdsPorUsuario(@Param("usuarioId") Long usuarioId, @Param("estado") EstadoReserva estado);
}
