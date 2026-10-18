package com.banduairlines.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.enums.ClaseCabina;

public interface TarifaVueloRepository
        extends JpaRepository<TarifaVuelo, Long> {

    @Query("""
            select tarifa
            from TarifaVuelo tarifa
            join fetch tarifa.vuelo vuelo
            join fetch tarifa.pack pack
            where vuelo.id = :vueloId
              and (:claseCabina is null
                   or tarifa.claseCabina = :claseCabina)
            order by tarifa.claseCabina, pack.nombre, tarifa.id
            """)
    List<TarifaVuelo> listarAdministracion(
            @Param("vueloId") Long vueloId,
            @Param("claseCabina") ClaseCabina claseCabina);

    @Query("""
            select tarifa
            from TarifaVuelo tarifa
            join fetch tarifa.vuelo vuelo
            join fetch tarifa.pack
            where vuelo.id = :vueloId
              and tarifa.id = :id
            """)
    Optional<TarifaVuelo> buscarAdministracion(
            @Param("vueloId") Long vueloId,
            @Param("id") Long id);

    @Query("""
            select count(tarifa)
            from TarifaVuelo tarifa
            where tarifa.vuelo.id = :vueloId
              and tarifa.claseCabina = :claseCabina
              and tarifa.pack.id = :packId
            """)
    long contarDuplicadasAdministracion(
            @Param("vueloId") Long vueloId,
            @Param("claseCabina") ClaseCabina claseCabina,
            @Param("packId") Long packId);
}