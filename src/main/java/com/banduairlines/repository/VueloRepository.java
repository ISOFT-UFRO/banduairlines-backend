package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Vuelo;
import com.banduairlines.dto.reportes.OcupacionVueloProjection;
import java.time.LocalDateTime;
import java.util.List;

public interface VueloRepository extends JpaRepository<Vuelo, Long>{
    @Query(value = "select v.codigo_vuelo as \"codigoVuelo\", ao.ciudad as origen, " +
            "ad.ciudad as destino, v.salida_programada as \"salidaProgramada\", " +
            "count(distinct case when r.estado = 'CONFIRMADA' and b.asiento_id is not null then b.id end) " +
            "as \"asientosOcupados\", " +
            "(select count(*) from asientos a where a.aeronave_id = v.aeronave_id) as capacidad " +
            "from vuelos v join rutas ru on ru.id = v.ruta_id " +
            "join aeropuertos ao on ao.id = ru.origen_id join aeropuertos ad on ad.id = ru.destino_id " +
            "left join boletos b on b.vuelo_id = v.id left join reservas r on r.id = b.reserva_id " +
            "where v.salida_programada >= :desde and v.salida_programada < :hasta " +
            "group by v.id, v.codigo_vuelo, ao.ciudad, ad.ciudad, v.salida_programada, v.aeronave_id " +
            "order by v.salida_programada", nativeQuery = true)
    List<OcupacionVueloProjection> resumirOcupacion(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
