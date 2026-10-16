package com.banduairlines.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.BoletoExtra;
import com.banduairlines.model.BoletoExtraId;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoPasajero;

public interface BoletoExtraRepository extends JpaRepository<BoletoExtra, BoletoExtraId>{
    @Query("""
        select
            tv.pack.id,
            tv.claseCabina,
            coalesce(sum(be.precioUnitario * be.cantidad), 0)
        from BoletoExtra be
        join be.boleto b
        join b.reserva r
        join b.vueloId v
        join b.tarifaVueloId tv
        join b.pasajeroId ps
        where (:inicio is null or r.createdAt >= :inicio)
          and (:fin is null or r.createdAt < :fin)
          and (:rutaId is null or v.ruta.id = :rutaId)
          and (:origenIata is null or upper(v.ruta.origen.codigoIata) = :origenIata)
          and (:destinoIata is null or upper(v.ruta.destino.codigoIata) = :destinoIata)
          and (:estadoReserva is null or r.estado = :estadoReserva)
          and (:tipoPasajero is null or ps.tipo = :tipoPasajero)
          and (:packId is null or tv.pack.id = :packId)
          and (:claseCabina is null or tv.claseCabina = :claseCabina)
          and (
                :estadoPago is null
                or exists (
                    select p.id from Pago p
                    where p.reserva = r and p.estado = :estadoPago
                )
          )
        group by tv.pack.id, tv.claseCabina
        """)
    List<Object[]> ingresosExtrasPorTarifaReportes(
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
