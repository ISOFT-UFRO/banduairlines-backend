package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

import com.banduairlines.model.Boleto;

public interface BoletoRepository extends JpaRepository<Boleto, Long>{
    @Query("""
            select distinct b.vueloId.id
            from Boleto b
            join b.reserva r
            where r.usuario.id = :idUsuario
              and r.estado = com.banduairlines.model.enums.EstadoReserva.CONFIRMADA
            order by b.vueloId.id
            """)
    List<Long> buscarIdsVuelosConfirmados(@Param("idUsuario") Long idUsuario);
    
}
