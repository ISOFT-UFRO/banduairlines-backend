package com.banduairlines.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.model.enums.EstadoVuelo;

public interface SeguimientoVueloRepository extends JpaRepository<SeguimientoVuelo, Long> {

    Optional<SeguimientoVuelo> findByVueloId(Long vueloId);

    @Query("select s from SeguimientoVuelo s left join fetch s.eventos where s.vueloId = :vueloId")
    Optional<SeguimientoVuelo> buscarConHistorialPorVueloId(@Param("vueloId") Long vueloId);

    @Query("select s from SeguimientoVuelo s left join fetch s.eventos where s.id = :id")
    Optional<SeguimientoVuelo> buscarConHistorialPorId(@Param("id") Long id);

    // El código puede repetirse: quien consulta elige el seguimiento vigente.
    List<SeguimientoVuelo> findByCodigoVuelo(String codigoVuelo);

    List<SeguimientoVuelo> findByVueloIdInOrderBySalidaProgramadaAsc(Collection<Long> vueloIds);

    List<SeguimientoVuelo> findByEstadoActualNotIn(Collection<EstadoVuelo> estados);

    @Query("""
            select s from SeguimientoVuelo s
            where s.salidaProgramada >= :desde and s.salidaProgramada < :hasta
            order by s.salidaProgramada asc
            """)
    List<SeguimientoVuelo> tableroEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
            select s from SeguimientoVuelo s
            where s.salidaProgramada >= :desde and s.salidaProgramada < :hasta
              and s.estadoActual = :estado
            order by s.salidaProgramada asc
            """)
    List<SeguimientoVuelo> tableroEntrePorEstado(@Param("desde") LocalDateTime desde,
                                                @Param("hasta") LocalDateTime hasta,
                                                @Param("estado") EstadoVuelo estado);

    default List<SeguimientoVuelo> tablero(LocalDateTime desde, LocalDateTime hasta, EstadoVuelo estado) {
        return estado == null ? tableroEntre(desde, hasta) : tableroEntrePorEstado(desde, hasta, estado);
    }
}
