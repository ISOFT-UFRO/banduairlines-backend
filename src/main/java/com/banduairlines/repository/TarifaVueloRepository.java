package com.banduairlines.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.enums.ClaseCabina;

public interface TarifaVueloRepository extends JpaRepository<TarifaVuelo, Long> {

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

    interface PrecioMinimoVuelo {
        Long getVueloId();
        BigDecimal getPrecioDesde();
    }

    @Query("""
            SELECT t FROM TarifaVuelo t
            JOIN FETCH t.vuelo v
            JOIN FETCH t.pack p
            WHERE v.id = :vueloId
              AND p.activo = true
            ORDER BY t.precio ASC
            """)
    List<TarifaVuelo> buscarTarifasActivasPorVuelo(@Param("vueloId") Long vueloId);

    @Query("""
            SELECT t.vuelo.id as vueloId, min(t.precio) as precioDesde
            FROM TarifaVuelo t
            WHERE t.vuelo.id IN :vueloIds
              AND t.pack.activo = true
            GROUP BY t.vuelo.id
            """)
    List<PrecioMinimoVuelo> buscarPreciosMinimos(@Param("vueloIds") Collection<Long> vueloIds);

    @Query("""
            SELECT t FROM TarifaVuelo t
            JOIN FETCH t.vuelo v
            JOIN FETCH t.pack p
            WHERE v.id IN :vueloIds
              AND p.activo = true
              AND t.precio >= 0
            ORDER BY t.precio ASC, t.id ASC
            """)
    List<TarifaVuelo> buscarTarifasActivasPorVuelos(@Param("vueloIds") List<Long> vueloIds);
}