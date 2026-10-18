package com.banduairlines.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Vuelo;
import com.banduairlines.service.tracking.puerto.VueloExterno;

// Lectura de Vuelo propia de Tracking, para no modificar VueloRepository (Búsqueda).
public interface TrackingVueloLecturaRepository extends Repository<Vuelo, Long> {

    @Query("""
            select new com.banduairlines.service.tracking.puerto.VueloExterno(
                v.id, v.codigoVuelo, o.codigoIata, d.codigoIata, v.salidaProgramada, v.llegadaProgramada)
            from Vuelo v
            join v.ruta r
            join r.origen o
            join r.destino d
            where v.salidaProgramada >= :desde and v.salidaProgramada < :hasta
            """)
    List<VueloExterno> listarEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
