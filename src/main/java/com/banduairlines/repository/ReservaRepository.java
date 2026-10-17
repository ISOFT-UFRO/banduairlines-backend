package com.banduairlines.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;

import jakarta.persistence.LockModeType;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // Endpoints 7 al 12 (compañero)

    boolean existsByCodigoReserva(String codigoReserva);

    List<Reserva> findByEstadoInAndExpiraEnBefore(Collection<EstadoReserva> estados, LocalDateTime limite);

    // Endpoints 13 al 16 

    // Bloquea la fila hasta el fin de la transacción (endpoints 13 y 16)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> findByIdForUpdate(@Param("id") Long id);

    //Endpoint 14. El código se genera en mayúsculas.
    Optional<Reserva> findByCodigoReserva(String codigoReserva);

    //Endpoint 15: historial del usuario, de la más reciente a la más antigua
    Page<Reserva> findByUsuario_IdOrderByCreatedAtDescIdDesc(Long usuarioId, Pageable pageable);

    Page<Reserva> findByUsuario_IdAndEstadoOrderByCreatedAtDescIdDesc(Long usuarioId, EstadoReserva estado, Pageable pageable);
//pago repository
    Optional<Reserva> findByCodigoReservaIgnoreCase(String codigoReserva);
}
