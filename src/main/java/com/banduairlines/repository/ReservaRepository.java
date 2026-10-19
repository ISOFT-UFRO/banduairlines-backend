package com.banduairlines.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.dto.reportes.IngresoRutaProjection;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoPasajero;

import jakarta.persistence.LockModeType;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Optional<Reserva> findByCodigoReservaAndUsuario_Id(String codigoReserva, Long idUsuario);

    // PAGOS
    boolean existsByCodigoReserva(String codigoReserva);

    List<Reserva> findByEstadoInAndExpiraEnBefore(
            Collection<EstadoReserva> estados,
            LocalDateTime limite);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> findByIdForUpdate(@Param("id") Long id);

    Optional<Reserva> findByCodigoReserva(String codigoReserva);

    Page<Reserva> findByUsuario_IdOrderByCreatedAtDescIdDesc(
            Long usuarioId,
            Pageable pageable);

    Page<Reserva> findByUsuario_IdAndEstadoOrderByCreatedAtDescIdDesc(
            Long usuarioId,
            EstadoReserva estado,
            Pageable pageable);

    Optional<Reserva> findByCodigoReservaIgnoreCase(String codigoReserva);

    // CHECK-IN
    Optional<Reserva> findByCodigoReservaAndEmailContacto(String codigoReserva, String emailContacto);

    List<Reserva> findByUsuarioId(Long usuarioId);

    // REPORTES
    @Query("select r.estado as estado, count(r.id) as cantidad from Reserva r " +
            "where r.createdAt >= :desde and r.createdAt < :hasta group by r.estado")
    List<ReservaEstadoProjection> contarPorEstadoReporte(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("select count(r.id) from Reserva r where r.estado = :estado " +
            "and r.createdAt >= :desde and r.createdAt < :hasta")
    long contarReservasReporte(
            @Param("estado") EstadoReserva estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query(value = "select ao.ciudad as origen, ad.ciudad as destino, r.moneda as moneda, " +
            "sum(b.precio_tarifa) as ingresos, count(distinct b.id) as boletos " +
            "from boletos b join reservas r on r.id = b.reserva_id " +
            "join vuelos v on v.id = b.vuelo_id join rutas ru on ru.id = v.ruta_id " +
            "join aeropuertos ao on ao.id = ru.origen_id join aeropuertos ad on ad.id = ru.destino_id " +
            "where r.estado = 'CONFIRMADA' and exists (select 1 from pagos p " +
            "where p.reserva_id = r.id and p.estado = 'PAGADO' " +
            "and p.fecha_pago >= :desde and p.fecha_pago < :hasta) " +
            "group by ao.ciudad, ad.ciudad, r.moneda order by ingresos desc", nativeQuery = true)
    List<IngresoRutaProjection> resumirIngresosPorRuta(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

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

    @Query("""
            select distinct year(r.createdAt), month(r.createdAt)
            from Reserva r
            where r.createdAt < :inicioMesActual
            order by year(r.createdAt) desc, month(r.createdAt) desc
            """)
    List<Object[]> buscarMesesHistoricosReportes(
            @Param("inicioMesActual") LocalDateTime inicioMesActual);

    interface ReservaEstadoProjection {
        EstadoReserva getEstado();
        Long getCantidad();
    }
}