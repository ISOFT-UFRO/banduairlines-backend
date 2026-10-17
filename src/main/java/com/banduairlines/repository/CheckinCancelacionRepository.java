package com.banduairlines.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Collection;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Boleto;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoPago;

import jakarta.persistence.LockModeType;

public interface CheckinCancelacionRepository extends Repository<Reserva, Long> {

    Optional <Reserva> findById(Long id);
    Optional <Reserva> findByCodigoReserva(String codigoReserva);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> bloquearPorId(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.codigoReserva = :codigo")
    Optional<Reserva> bloquearPorCodigo(@Param("codigo") String codigo);

    @Query("select p.apellido from Pasajero p where p.reserva.id = :reservaId")
    List<String> buscarApellidosPasajero(@Param("reservaId") Long reservaId);

    @Query("""
            select b from Boleto b
                join fetch b.pasajeroId
                join fetch b.vueloId v
                join fetch v.ruta ru
                join fetch ru.origen
                join fetch ru.destino
                join fetch b.tarifaVueloId t
                join fetch t.pack
            where b.reserva.id = :reservaId
            order by v.salidaProgramada, b.ordenTramo, b.id
            """)
    List<Boleto> buscarBoletosConDetalle(@Param("reservaId") Long reservaId);

    @Query("select t.boletoId from TarjetaEmbarque t where t.boleto.reserva.id = :reservaId")
    Set<Long> buscarBoletosConCheckin(@Param("reservaId") Long reservaId);

    @Query("select sum(p.monto) from Pago p where p.reserva.id = :reservaId and p.estado = :estado")
    BigDecimal sumarPagos(@Param("reservaId")Long reservaId, @Param("estado") EstadoPago estado);

    @Query("""
            select case when count(p) > 0 then true else false end
            from Promocion p
            where p.codigo like concat(:prefijo, '%')
            """)
    boolean existeCuponConPrefijo(@Param("prefijo") String prefijo);

    interface EstadoVueloTramo {
        Long getVueloId();
        String getEstadoActual();
    }
    @Query(value = """
            select s.vuelo_id as "vueloId", s.estado_actual as "estadoActual"
            from seguimiento_vuelo s
            where s.vuelo_id in (:vueloIds)
            """, nativeQuery = true)
    List<EstadoVueloTramo> buscarEstadosVuelo(@Param("vueloIds") Collection<Long> vueloIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from TarjetaEmbarque t where t.boletoId in (select b.id from Boleto b where b.reserva.id = :reservaId)")
    int eliminarTarjetasEmbarque(@Param("reservaId") Long reservaId);

}
