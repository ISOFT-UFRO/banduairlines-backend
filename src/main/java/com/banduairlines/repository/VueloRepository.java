package com.banduairlines.repository;

import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.Estado;
import com.banduairlines.model.enums.EstadoVuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VueloRepository extends JpaRepository<Vuelo, Long> {

    @Query("""
            SELECT v FROM Vuelo v
            JOIN FETCH v.ruta r
            JOIN FETCH r.origen o
            JOIN FETCH r.destino d
            JOIN FETCH v.aeronave a
            WHERE UPPER(o.codigoIata) = UPPER(:origenIata)
              AND UPPER(d.codigoIata) = UPPER(:destinoIata)
              AND v.salidaProgramada >= :inicioDia
              AND v.salidaProgramada < :finDia
              AND v.estado IN :estados
            ORDER BY v.salidaProgramada ASC
            """)
    List<Vuelo> buscarVuelosDirectos(
            @Param("origenIata") String origenIata,
            @Param("destinoIata") String destinoIata,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia,
            @Param("estados") Collection<EstadoVuelo> estados
    );

    @Query("""
            SELECT v FROM Vuelo v
            JOIN FETCH v.ruta r
            JOIN FETCH r.origen o
            JOIN FETCH r.destino d
            JOIN FETCH v.aeronave a
            WHERE UPPER(o.codigoIata) = UPPER(:origenIata)
              AND UPPER(d.codigoIata) <> UPPER(:destinoIata)
              AND v.salidaProgramada >= :inicioDia
              AND v.salidaProgramada < :finDia
              AND v.estado IN :estados
            ORDER BY v.salidaProgramada ASC
            """)
    List<Vuelo> buscarPrimerosTramosParaConexion(
            @Param("origenIata") String origenIata,
            @Param("destinoIata") String destinoIata,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia,
            @Param("estados") Collection<EstadoVuelo> estados
    );

    @Query("""
            SELECT v FROM Vuelo v
            JOIN FETCH v.ruta r
            JOIN FETCH r.origen o
            JOIN FETCH r.destino d
            JOIN FETCH v.aeronave a
            WHERE UPPER(d.codigoIata) = UPPER(:destinoIata)
              AND UPPER(o.codigoIata) <> UPPER(:origenIata)
              AND v.salidaProgramada >= :desdeSalida
              AND v.salidaProgramada <= COALESCE(:hastaSalida, v.salidaProgramada)
              AND v.estado IN :estados
            ORDER BY v.salidaProgramada ASC
            """)
    List<Vuelo> buscarSegundosTramosParaConexion(
            @Param("origenIata") String origenIata,
            @Param("destinoIata") String destinoIata,
            @Param("desdeSalida") LocalDateTime desdeSalida,
            @Param("hastaSalida") LocalDateTime hastaSalida,
            @Param("estados") Collection<EstadoVuelo> estados
    );

    @Query("""
            SELECT v FROM Vuelo v
            JOIN FETCH v.ruta r
            JOIN FETCH r.origen o
            JOIN FETCH r.destino d
            JOIN FETCH v.aeronave a
            WHERE v.id = :id
            """)
    Optional<Vuelo> findByIdConDetalles(@Param("id") Long id);

    @Query("""
            SELECT v FROM Vuelo v
            JOIN FETCH v.ruta r
            JOIN FETCH r.origen o
            JOIN FETCH r.destino d
            JOIN FETCH v.aeronave a
            WHERE UPPER(v.codigoVuelo) = UPPER(:codigoVuelo)
            """)
    Optional<Vuelo> findByCodigoVueloConDetalles(@Param("codigoVuelo") String codigoVuelo);

    @Query("""
            SELECT v FROM Vuelo v
            JOIN FETCH v.ruta r
            JOIN FETCH r.origen o
            JOIN FETCH r.destino d
            JOIN FETCH v.aeronave a
            WHERE v.salidaProgramada >= :inicioDia
              AND v.salidaProgramada < :finDia
            ORDER BY v.salidaProgramada ASC
            """)
    List<Vuelo> buscarPorRangoFecha(
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia
    );

    @Query("""
            SELECT COUNT(v) > 0 FROM Vuelo v
            WHERE v.aeronave.id = :aeronaveId
              AND v.estado IN :estadosActivos
              AND (:vueloIdExcluir IS NULL OR v.id <> :vueloIdExcluir)
              AND v.salidaProgramada < :llegadaProgramada
              AND v.llegadaProgramada > :salidaProgramada
            """)
    boolean existeSolapamientoAeronave(
            @Param("aeronaveId") Long aeronaveId,
            @Param("salidaProgramada") LocalDateTime salidaProgramada,
            @Param("llegadaProgramada") LocalDateTime llegadaProgramada,
            @Param("estadosActivos") Collection<EstadoVuelo> estadosActivos,
            @Param("vueloIdExcluir") Long vueloIdExcluir
    );

    @Query("""
            select v from Vuelo v
            join fetch v.ruta r
            join fetch r.origen o
            join fetch r.destino d
            where o.id = :origenId and d.id = :destinoId
              and v.salidaProgramada >= :inicio and v.salidaProgramada < :fin
              and v.salidaProgramada > :ahora
              and r.activa = true and v.aeronave.estado = :estadoAeronave
              and v.estado in :estados
            order by v.salidaProgramada, v.id
            """)
    List<Vuelo> buscarOfrecibles(@Param("origenId") Long origenId,
            @Param("destinoId") Long destinoId, @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin, @Param("ahora") LocalDateTime ahora,
            @Param("estados") Collection<EstadoVuelo> estados,
            @Param("estadoAeronave") Estado estadoAeronave);

    @Query("""
            select v from Vuelo v
            join fetch v.ruta r
            join fetch r.origen o
            join fetch r.destino d
            where v.salidaProgramada >= :inicio and v.salidaProgramada < :fin
              and r.activa = true and v.aeronave.estado = :estadoAeronave
              and v.estado in :estados
            order by v.salidaProgramada, v.id
            """)
    List<Vuelo> buscarCandidatosOfrecibles(@Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estados") Collection<EstadoVuelo> estados,
            @Param("estadoAeronave") Estado estadoAeronave);
    @Query("""
            SELECT COUNT(v) > 0 FROM Vuelo v
            WHERE UPPER(v.codigoVuelo) = UPPER(:codigoVuelo)
              AND v.salidaProgramada = :salidaProgramada
              AND (:vueloIdExcluir IS NULL OR v.id <> :vueloIdExcluir)
            """)
    boolean existePorCodigoVueloYSalida(
            @Param("codigoVuelo") String codigoVuelo,
            @Param("salidaProgramada") LocalDateTime salidaProgramada,
            @Param("vueloIdExcluir") Long vueloIdExcluir
    );
}
