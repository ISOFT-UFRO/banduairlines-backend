package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.Estado;

import com.banduairlines.model.Vuelo;

public interface VueloRepository extends JpaRepository<Vuelo, Long>{
    
    @Query("""
            select v from Vuelo v
            join fetch v.ruta r
            join fetch r.origen o
            join fetch r.destino d
            where o.id = :origenId and d.id = :destinoId
              and v.salidaProgramada >= :inicio and v.salidaProgramada < :fin
              and v.salidaProgramada > :ahora
              and r.activa = true and v.aeronave.estado = :estadoAeronave
              and v.estado in :estados
            order by v.salidaProgramada, v.id
            """)
    List<Vuelo> buscarOfrecibles(@Param("origenId") Long origenId,
            @Param("destinoId") Long destinoId, @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin, @Param("ahora") LocalDateTime ahora,
            @Param("estados") Collection<EstadoVuelo> estados,
            @Param("estadoAeronave") Estado estadoAeronave);
}
