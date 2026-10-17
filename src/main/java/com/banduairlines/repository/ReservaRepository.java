package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Reserva;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long>{
    Optional<Reserva> findByCodigoReservaAndEmailContacto(String codigoReserva, String emailContacto);
    List<Reserva> findByUsuarioId(Long usuarioId);
}
