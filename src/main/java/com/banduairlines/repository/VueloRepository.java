package com.banduairlines.repository;

import com.banduairlines.model.Vuelo;
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
}
