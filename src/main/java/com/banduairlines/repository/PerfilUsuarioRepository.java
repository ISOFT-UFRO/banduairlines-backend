package com.banduairlines.repository;

import com.banduairlines.model.Usuario;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface PerfilUsuarioRepository extends Repository<Usuario, Long> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Usuario u
            set u.nombre = coalesce(:nombres, u.nombre),
                u.apellido = coalesce(:apellidos, u.apellido),
                u.updatedAt = :fecha
            where u.id = :idUsuario and u.activo = true
              and u.rol in (com.banduairlines.model.enums.RolUsuario.PASAJERO,
                            com.banduairlines.model.enums.RolUsuario.ADMINISTRADOR)
            """)
    int actualizarPerfil(@Param("idUsuario") Long idUsuario, @Param("nombres") String nombres,
                         @Param("apellidos") String apellidos, @Param("fecha") LocalDateTime fecha);
}
