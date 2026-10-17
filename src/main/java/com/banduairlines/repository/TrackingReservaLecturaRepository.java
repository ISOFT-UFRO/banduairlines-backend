package com.banduairlines.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Reserva;
import com.banduairlines.service.tracking.puerto.ReservaExterna;

// Lectura de Reserva propia de Tracking (RF-04), para no modificar ReservaRepository (Reserva).
public interface TrackingReservaLecturaRepository extends Repository<Reserva, Long> {

    @Query("""
            select new com.banduairlines.service.tracking.puerto.ReservaExterna(r.id, r.estado)
            from Reserva r
            where r.codigoReserva = :codigoReserva
            """)
    Optional<ReservaExterna> buscarPorCodigo(@Param("codigoReserva") String codigoReserva);

    @Query("select r.id from Reserva r where r.usuario.id = :usuarioId")
    List<Long> buscarIdsPorUsuario(@Param("usuarioId") Long usuarioId);
}
