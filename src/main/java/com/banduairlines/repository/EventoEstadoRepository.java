package com.banduairlines.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import com.banduairlines.model.EventoEstado;

// Repository y no JpaRepository: no hereda operaciones de borrado (RNF-03).
public interface EventoEstadoRepository extends Repository<EventoEstado, Long> {

    EventoEstado save(EventoEstado evento);

    List<EventoEstado> findBySeguimientoIdOrderByFechaCambioDescIdDesc(Long seguimientoId);
}
