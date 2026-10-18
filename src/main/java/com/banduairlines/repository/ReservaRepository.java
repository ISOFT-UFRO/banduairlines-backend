package com.banduairlines.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.Reserva;

import jakarta.persistence.LockModeType;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    java.util.Optional<Reserva> findByCodigoReservaAndUsuario_Id(String codigoReserva, Long idUsuario);

    boolean existsByCodigoReserva(String codigoReserva);

    List<Reserva> findByEstadoInAndExpiraEnBefore(
            Collection<EstadoReserva> estados,
            LocalDateTime limite);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> findByIdForUpdate(@Param("id") Long id);

    Optional<Reserva> findByCodigoReserva(String codigoReserva);

    Page<Reserva> findByUsuario_IdOrderByCreatedAtDescIdDesc(
            Long usuarioId,
            Pageable pageable);

    Page<Reserva> findByUsuario_IdAndEstadoOrderByCreatedAtDescIdDesc(
            Long usuarioId,
            EstadoReserva estado,
            Pageable pageable);

    Optional<Reserva> findByCodigoReservaIgnoreCase(String codigoReserva);
}
