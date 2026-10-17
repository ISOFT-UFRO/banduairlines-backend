package com.banduairlines.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.dto.reportes.OcupacionVueloProjection;
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
            @Query("""
                select count(a.id)
                from Vuelo v, Asiento a
                where a.aeronave = v.aeronave
                and (:inicio is null or v.salidaProgramada >= :inicio)
                and (:fin is null or v.salidaProgramada < :fin)
                and (:rutaId is null or v.ruta.id = :rutaId)
                and (:origenIata is null or upper(v.ruta.origen.codigoIata) = :origenIata)
                and (:destinoIata is null or upper(v.ruta.destino.codigoIata) = :destinoIata)
                and (
                (
                    :estadoReserva is null
                    and :estadoPago is null
                    and :tipoPasajero is null
                    and :packId is null
                    and :claseCabina is null
                )
                or exists (
                    select b.id
                    from Boleto b
                    join b.reserva r
                    join b.tarifaVueloId tv
                    join b.pasajeroId ps
                    where b.vueloId = v
                      and (:estadoReserva is null or r.estado = :estadoReserva)
                      and (:tipoPasajero is null or ps.tipo = :tipoPasajero)
                      and (:packId is null or tv.pack.id = :packId)
                      and (:claseCabina is null or tv.claseCabina = :claseCabina)
                      and (
                            :estadoPago is null
                            or exists (
                                select p.id
                                from Pago p
                                where p.reserva = r and p.estado = :estadoPago
                            )
                        )
                    )
                )
            """)
    Long contarCapacidadAsientosReportes(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("rutaId") Long rutaId,
            @Param("origenIata") String origenIata,
            @Param("destinoIata") String destinoIata,
            @Param("estadoReserva") EstadoReserva estadoReserva,
            @Param("estadoPago") EstadoPago estadoPago,
            @Param("tipoPasajero") TipoPasajero tipoPasajero,
            @Param("packId") Long packId,
            @Param("claseCabina") ClaseCabina claseCabina);
}
