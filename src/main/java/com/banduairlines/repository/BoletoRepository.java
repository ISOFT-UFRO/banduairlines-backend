package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Boleto;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.dto.reportes.PasajeroTipoProjection;
import com.banduairlines.dto.reportes.VentaExtraProjection;
import java.time.LocalDateTime;
import java.util.List;

public interface BoletoRepository extends JpaRepository<Boleto, Long>{
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
}
