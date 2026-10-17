package com.banduairlines.repository;

import com.banduairlines.model.TarifaVuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import java.util.List;

public interface TarifaVueloRepository extends JpaRepository<TarifaVuelo, Long> {

    @Query("""
            SELECT t FROM TarifaVuelo t
            JOIN FETCH t.vuelo v
            JOIN FETCH t.pack p
            WHERE v.id = :vueloId
              AND p.activo = true
            ORDER BY t.precio ASC
            """)
    List<TarifaVuelo> buscarTarifasActivasPorVuelo(@Param("vueloId") Long vueloId);

public interface TarifaVueloRepository extends JpaRepository<TarifaVuelo, Long>{
    interface PrecioMinimoVuelo {
        Long getVueloId();
        BigDecimal getPrecioDesde();
    }

    @Query("""
            select t.vuelo.id as vueloId, min(t.precio) as precioDesde
            from TarifaVuelo t
            where t.vuelo.id in :vueloIds and t.pack.activo = true
            group by t.vuelo.id
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
