package com.banduairlines.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.PackTarifario;

public interface PackTarifarioRepository
        extends JpaRepository<PackTarifario, Long> {

    @Query("""
            select pack
            from PackTarifario pack
            where (:activo is null or pack.activo = :activo)
            order by pack.nombre, pack.id
            """)
    List<PackTarifario> listarAdministracion(
            @Param("activo") Boolean activo);

    @Query("""
            select count(pack)
            from PackTarifario pack
            where lower(trim(pack.nombre)) = lower(trim(:nombre))
              and (:excluirId is null or pack.id <> :excluirId)
            """)
    long contarDuplicadosAdministracion(
            @Param("nombre") String nombre,
            @Param("excluirId") Long excluirId);
}