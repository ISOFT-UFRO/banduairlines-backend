package com.banduairlines.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Pago;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoPasajero;

public interface PagoRepository extends JpaRepository<Pago, Long>{
    @Query(value = "select cast(p.fecha_pago as date) as fecha, p.moneda as moneda, " +
            "sum(p.monto) as ingresos, count(*) as pagos from pagos p " +
            "where p.estado = 'PAGADO' and p.fecha_pago >= :desde and p.fecha_pago < :hasta " +
            "group by cast(p.fecha_pago as date), p.moneda order by fecha, moneda", nativeQuery = true)
            
    List<IngresoDiarioProjection> obtenerIngresosDiarios(
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select sum(p.monto) from Pago p where p.estado = :estado and p.moneda = :moneda " +
            "and p.fechaPago >= :desde and p.fechaPago < :hasta")
    java.math.BigDecimal sumarIngresosReporte(@Param("estado") EstadoPago estado,
            @Param("moneda") String moneda, @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query(value = "select p.moneda as moneda, sum(p.monto) as ingresos from pagos p " +
            "where p.estado = 'PAGADO' and p.fecha_pago >= :desde and p.fecha_pago < :hasta " +
            "group by p.moneda order by p.moneda", nativeQuery = true)
    List<com.banduairlines.dto.reportes.IngresoPorMonedaProjection> sumarIngresosPorMoneda(
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(value = "select count(*) from pagos p where p.estado = 'PAGADO' " +
            "and p.fecha_pago >= :desde and p.fecha_pago < :hasta", nativeQuery = true)
    long contarPagosConfirmados(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

        @Query("""
        select coalesce(sum(p.monto), 0)
        from Pago p
        join p.reserva r
        where p.estado = com.banduairlines.model.enums.EstadoPago.PAGADO
          and (:inicio is null or coalesce(p.fechaPago, p.createdAt) >= :inicio)
          and (:fin is null or coalesce(p.fechaPago, p.createdAt) < :fin)
          and (:estadoReserva is null or r.estado = :estadoReserva)
          and (:estadoPago is null or p.estado = :estadoPago)
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
    """)
    BigDecimal sumarIngresosPagadosReportes(
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
        select p.estado, count(distinct p.id), coalesce(sum(p.monto), 0)
        from Pago p
        join p.reserva r
        where (:inicio is null or coalesce(p.fechaPago, p.createdAt) >= :inicio)
          and (:fin is null or coalesce(p.fechaPago, p.createdAt) < :fin)
          and (:estadoReserva is null or r.estado = :estadoReserva)
          and (:estadoPago is null or p.estado = :estadoPago)
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
        group by p.estado
        order by p.estado
    """)
    List<Object[]> estadosPagoReportes(
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
        select coalesce(p.fechaPago, p.createdAt), coalesce(sum(p.monto), 0)
        from Pago p
        join p.reserva r
        where p.estado = com.banduairlines.model.enums.EstadoPago.PAGADO
          and (:inicio is null or coalesce(p.fechaPago, p.createdAt) >= :inicio)
          and (:fin is null or coalesce(p.fechaPago, p.createdAt) < :fin)
          and (:estadoReserva is null or r.estado = :estadoReserva)
          and (:estadoPago is null or p.estado = :estadoPago)
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
        group by coalesce(p.fechaPago, p.createdAt)
        order by coalesce(p.fechaPago, p.createdAt)
    """)
    List<Object[]> ingresosPorFechaReportes(
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
    select distinct
        year(coalesce(p.fechaPago, p.createdAt)),
        month(coalesce(p.fechaPago, p.createdAt))
    from Pago p
    where coalesce(p.fechaPago, p.createdAt) < :inicioMesActual
    order by
        year(coalesce(p.fechaPago, p.createdAt)) desc,
        month(coalesce(p.fechaPago, p.createdAt)) desc
    """)
    List<Object[]> buscarMesesHistoricosReportes(
        @Param("inicioMesActual") LocalDateTime inicioMesActual);

        

    interface IngresoDiarioProjection {

        LocalDate getFecha();

        BigDecimal getIngresos();
    }
import com.banduairlines.model.enums.EstadoPago;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByFlowToken(String flowToken);

    Optional<Pago> findByFlowOrder(Long flowOrder);

    List<Pago> findByReservaIdOrderByCreatedAtDesc(Long reservaId);

    Optional<Pago> findFirstByReservaIdAndEstadoOrderByIdDesc(Long reservaId, EstadoPago estado);

    /**
     * Busca el pago por el token del reembolso que Pagos guarda en
     * detalle_respuesta (PostgreSQL).
     */
    @Query(value = "SELECT * FROM pagos WHERE CAST(detalle_respuesta AS jsonb) -> 'reembolso' ->> 'token' = :token LIMIT 1", nativeQuery = true)
    Optional<Pago> findByReembolsoToken(@Param("token") String token);

    /**
     * Sin parámetros nulos: el servicio usa límites amplios y '%' cuando el filtro
     * no viene.
     */
    @Query("SELECT p FROM Pago p WHERE p.reserva.usuario.id = :usuarioId AND p.estado = 'PAGADO' " +
            "AND p.fechaPago >= :desde AND p.fechaPago <= :hasta " +
            "AND UPPER(p.reserva.codigoReserva) LIKE :codigoReserva " +
            "ORDER BY p.fechaPago DESC, p.createdAt DESC")
    Page<Pago> findPagadosByUsuarioId(@Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("codigoReserva") String codigoReserva,
            Pageable pageable);
}
