package com.banduairlines.repository;

import com.banduairlines.model.Boleto;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoletoRepository extends JpaRepository<Boleto, Long> {

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