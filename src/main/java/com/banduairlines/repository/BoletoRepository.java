package com.banduairlines.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Boleto;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoPasajero;

import com.banduairlines.model.Pasajero;

public interface BoletoRepository extends JpaRepository<Boleto, Long>{

    /** RESERVA: boletos de una reserva ordenados por tramo. */
    List<Boleto> findByReserva_IdOrderByOrdenTramoAscIdAsc(Long reservaId);

    /**
     * RESERVA: plazas con asiento (los bebés no ocupan) tomadas en un vuelo y cabina por reservas confirmadas
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

    /** RESERVA: asientos de un vuelo tomados por otras reservas confirmadas o abiertas con el bloqueo vigente. */
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

    /** RESERVA: indica si el asiento ya está tomado en el vuelo por otra reserva en alguno de los estados dados. */
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
    //Cheking

    List<Boleto> findByPasajeroId(Pasajero pasajeroId);
}
