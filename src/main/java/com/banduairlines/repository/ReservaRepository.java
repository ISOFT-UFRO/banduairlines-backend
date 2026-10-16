package com.banduairlines.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoPasajero;

public interface ReservaRepository extends JpaRepository<Reserva, Long>{
    @Query("""
        select r.estado, count(distinct r.id), coalesce(sum(r.total), 0)
        from Reserva r
        where (:inicio is null or r.createdAt >= :inicio)
          and (:fin is null or r.createdAt < :fin)
          and (:estadoReserva is null or r.estado = :estadoReserva)
          and (
                :estadoPago is null
                or exists (
                    select p.id from Pago p
                    where p.reserva = r and p.estado = :estadoPago
                )
          )
          and (
                (
                    :rutaId is null
                    and :origenIata is null
                    and :destinoIata is null
                    and :tipoPasajero is null
                    and :packId is null
                    and :claseCabina is null
                )
                or exists (
                    select b.id
                    from Boleto b
                    join b.vueloId v
                    join b.tarifaVueloId tv
                    join b.pasajeroId ps
                    where b.reserva = r
                      and (:rutaId is null or v.ruta.id = :rutaId)
                      and (:origenIata is null or upper(v.ruta.origen.codigoIata) = :origenIata)
                      and (:destinoIata is null or upper(v.ruta.destino.codigoIata) = :destinoIata)
                      and (:tipoPasajero is null or ps.tipo = :tipoPasajero)
                      and (:packId is null or tv.pack.id = :packId)
                      and (:claseCabina is null or tv.claseCabina = :claseCabina)
                )
          )
        group by r.estado
        order by r.estado
    """)
    List<Object[]> estadosReservaReportes(
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
