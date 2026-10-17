package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import com.banduairlines.model.TarifaVuelo;

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
}
