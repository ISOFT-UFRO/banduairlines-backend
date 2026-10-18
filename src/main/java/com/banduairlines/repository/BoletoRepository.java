package com.banduairlines.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Boleto;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;

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
            @Param("estadoExcluido") EstadoReserva estadoExcluido
    );
}