package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.dto.reportes.ReservaEstadoProjection;
import com.banduairlines.dto.reportes.IngresoRutaProjection;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long>{
    @Query("select r.estado as estado, count(r.id) as cantidad from Reserva r " +
            "where r.createdAt >= :desde and r.createdAt < :hasta group by r.estado")
    List<ReservaEstadoProjection> contarPorEstadoReporte(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("select count(r.id) from Reserva r where r.estado = :estado " +
            "and r.createdAt >= :desde and r.createdAt < :hasta")
    long contarReservasReporte(@Param("estado") EstadoReserva estado,
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(value = "select ao.ciudad as origen, ad.ciudad as destino, r.moneda as moneda, " +
            "sum(b.precio_tarifa) as ingresos, count(distinct b.id) as boletos " +
            "from boletos b join reservas r on r.id = b.reserva_id " +
            "join vuelos v on v.id = b.vuelo_id join rutas ru on ru.id = v.ruta_id " +
            "join aeropuertos ao on ao.id = ru.origen_id join aeropuertos ad on ad.id = ru.destino_id " +
            "where r.estado = 'CONFIRMADA' and exists (select 1 from pagos p " +
            "where p.reserva_id = r.id and p.estado = 'PAGADO' " +
            "and p.fecha_pago >= :desde and p.fecha_pago < :hasta) " +
            "group by ao.ciudad, ad.ciudad, r.moneda order by ingresos desc", nativeQuery = true)
    List<IngresoRutaProjection> resumirIngresosPorRuta(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
