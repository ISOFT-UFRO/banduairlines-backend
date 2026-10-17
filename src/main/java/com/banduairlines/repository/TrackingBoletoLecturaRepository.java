package com.banduairlines.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Boleto;

// Lectura de Boleto propia de Tracking (RF-04), para no modificar BoletoRepository (Reserva).
public interface TrackingBoletoLecturaRepository extends Repository<Boleto, Long> {

    @Query("select distinct b.vueloId.id from Boleto b where b.reserva.id in :reservaIds")
    List<Long> buscarVueloIdsPorReservas(@Param("reservaIds") Collection<Long> reservaIds);
}
