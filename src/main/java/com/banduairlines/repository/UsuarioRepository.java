package com.banduairlines.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarParaActualizar(Long id);

    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);
}
