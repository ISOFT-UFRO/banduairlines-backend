package com.banduairlines.repository;

import com.banduairlines.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import java.util.Optional;

public interface CuentaPuntosRepository extends Repository<Usuario, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :idUsuario")
    Optional<Usuario> bloquear(Long idUsuario);
    Optional<Usuario> findById(Long idUsuario);
}
