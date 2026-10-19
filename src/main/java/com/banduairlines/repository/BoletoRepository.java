package com.banduairlines.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.dto.reportes.PasajeroTipoProjection;
import com.banduairlines.dto.reportes.VentaExtraProjection;

import com.banduairlines.model.Boleto;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoPasajero;

public interface BoletoRepository extends JpaRepository<Boleto, Long> {

        @Query("""
                        select distinct b.vueloId.id
                        from Boleto b
                        join b.reserva r
                        where r.usuario.id = :idUsuario
                          and r.estado = com.banduairlines.model.enums.EstadoReserva.CONFIRMADA
                        order by b.vueloId.id
                        """)
        List<Long> buscarIdsVuelosConfirmados(@Param("idUsuario") Long idUsuario);

        @Query("""
                        SELECT COUNT(b) FROM Boleto b
                        WHERE b.vueloId.id = :vueloId
                          AND b.tarifaVueloId.claseCabina = :claseCabina
                          AND b.reserva.estado <> :estadoExcluido
                        """)
        long contarOcupadosPorVueloYClaseCabina(
                        @Param("vueloId") Long vueloId,
                        @Param("claseCabina") ClaseCabina claseCabina,
                        @Param("estadoExcluido") EstadoReserva estadoExcluido);

        /** RESERVA: boletos de una reserva ordenados por tramo. */
        List<Boleto> findByReserva_IdOrderByOrdenTramoAscIdAsc(Long reservaId);

        /**
         * RESERVA: plazas con asiento (los bebés no ocupan) tomadas en un vuelo y
         * cabina por reservas confirmadas
         * o por compras abiertas cuyo bloqueo no ha vencido.
         */
        @Query("""
                        select count(b) from Boleto b
                        where b.vueloId.id = :vueloId
                          and b.tarifaVueloId.claseCabina = :cabina
                          and b.pasajeroId.tipo <> :bebe
                          and (b.reserva.estado = :confirmada
                               or (b.reserva.estado in :abiertas and b.reserva.expiraEn > :ahora))
                        """)
        long plazasTomadas(@Param("vueloId") Long vueloId,
                        @Param("cabina") ClaseCabina cabina,
                        @Param("bebe") TipoPasajero bebe,
                        @Param("confirmada") EstadoReserva confirmada,
                        @Param("abiertas") Collection<EstadoReserva> abiertas,
                        @Param("ahora") LocalDateTime ahora);

        /**
         * RESERVA: asientos de un vuelo tomados por otras reservas confirmadas o
         * abiertas con el bloqueo vigente.
         */
        @Query("""
                        select b.asientoId.id from Boleto b
                        where b.vueloId.id = :vueloId
                          and b.asientoId is not null
                          and b.reserva.id <> :reservaId
                          and (b.reserva.estado = :confirmada
                               or (b.reserva.estado in :abiertas and b.reserva.expiraEn > :ahora))
                        """)
        List<Long> asientosTomadosPorOtras(@Param("vueloId") Long vueloId,
                        @Param("reservaId") Long reservaId,
                        @Param("confirmada") EstadoReserva confirmada,
                        @Param("abiertas") Collection<EstadoReserva> abiertas,
                        @Param("ahora") LocalDateTime ahora);

        /**
         * RESERVA: indica si el asiento ya está tomado en el vuelo por otra reserva en
         * alguno de los estados dados.
         */
        @Query("""
                        select count(b) > 0 from Boleto b
                        where b.vueloId.id = :vueloId
                          and b.asientoId.id = :asientoId
                          and b.reserva.id <> :reservaId
                          and b.reserva.estado in :estados
                        """)
        boolean asientoOcupadoPorOtraReserva(@Param("vueloId") Long vueloId,
                        @Param("asientoId") Long asientoId,
                        @Param("reservaId") Long reservaId,
                        @Param("estados") Collection<EstadoReserva> estados);
        // Cheking

        List<Boleto> findByPasajeroId(Pasajero pasajeroId);

        @Query("select b.pasajeroId.tipo as tipo, count(distinct b.pasajeroId.id) as cantidad " +
                        "from Boleto b where b.reserva.estado = :estado and b.createdAt >= :desde " +
                        "and b.createdAt < :hasta group by b.pasajeroId.tipo")
        List<PasajeroTipoProjection> contarPorTipoReporte(@Param("estado") EstadoReserva estado,
                        @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

        @Query("select count(distinct b.id) from Boleto b where b.reserva.estado = :estado " +
                        "and b.asientoId is not null and b.createdAt >= :desde and b.createdAt < :hasta")
        long contarAsientosAsignadosReporte(@Param("estado") EstadoReserva estado,
                        @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

        @Query("select count(distinct b.pasajeroId.id) from Boleto b where b.reserva.estado = :estado " +
                        "and b.pasajeroId.tipo = :tipo and b.createdAt >= :desde and b.createdAt < :hasta")
        long contarPasajerosReporte(@Param("estado") EstadoReserva estado,
                        @Param("tipo") TipoPasajero tipo, @Param("desde") LocalDateTime desde,
                        @Param("hasta") LocalDateTime hasta);

        @Query(value = "select e.nombre as nombre, e.tipo as tipo, r.moneda as moneda, " +
                        "sum(be.precio_unitario * be.cantidad) as ingresos, sum(be.cantidad) as unidades " +
                        "from boleto_extras be join extras e on e.id = be.extra_id " +
                        "join boletos b on b.id = be.boleto_id join reservas r on r.id = b.reserva_id " +
                        "where r.estado = 'CONFIRMADA' and b.created_at >= :desde and b.created_at < :hasta " +
                        "group by e.id, e.nombre, e.tipo, r.moneda order by ingresos desc", nativeQuery = true)
        List<VentaExtraProjection> resumirVentasExtras(@Param("desde") LocalDateTime desde,
                        @Param("hasta") LocalDateTime hasta);

        @Query("""
                        select count(distinct b.id)
                        from Boleto b
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
                                    select p.id
                                    from Pago p
                                    where p.reserva = r
                                      and p.estado = :estadoPago
                                )
                          )
                        """)
        Long contarVentasReportes(
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

        @Query("""
                            select r.createdAt, count(distinct b.id)
                            from Boleto b
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
                            group by r.createdAt
                            order by r.createdAt
                        """)
        List<Object[]> ventasPorFechaReportes(
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

        @Query("""
                            select
                                v.ruta.id,
                                v.ruta.origen.codigoIata,
                                v.ruta.origen.nombre,
                                v.ruta.destino.codigoIata,
                                v.ruta.destino.nombre,
                                count(distinct b.id)
                            from Boleto b
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
                            group by
                                v.ruta.id,
                                v.ruta.origen.codigoIata,
                                v.ruta.origen.nombre,
                                v.ruta.destino.codigoIata,
                                v.ruta.destino.nombre
                            order by count(distinct b.id) desc
                        """)
        List<Object[]> rankingRutasReportes(
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

        @Query("""
                            select ps.tipo, count(distinct ps.id)
                            from Boleto b
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
                            group by ps.tipo
                            order by ps.tipo
                        """)
        List<Object[]> pasajerosPorTipoReportes(
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

        @Query("""
                            select
                                tv.pack.id,
                                tv.pack.nombre,
                                tv.claseCabina,
                                count(distinct b.id),
                                coalesce(sum(b.precioTarifa), 0)
                            from Boleto b
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
                            group by tv.pack.id, tv.pack.nombre, tv.claseCabina
                            order by tv.pack.nombre, tv.claseCabina
                        """)
        List<Object[]> tarifasPorPackYClaseReportes(
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

        @Query("""
                            select count(distinct b.id)
                            from Boleto b
                            join b.reserva r
                            join b.vueloId v
                            join b.tarifaVueloId tv
                            join b.pasajeroId ps
                            where b.asientoId is not null
                              and r.estado = com.banduairlines.model.enums.EstadoReserva.CONFIRMADA
                              and (:inicio is null or v.salidaProgramada >= :inicio)
                              and (:fin is null or v.salidaProgramada < :fin)
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
                        """)
        Long contarAsientosOcupadosReportes(
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
