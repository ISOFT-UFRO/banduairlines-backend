package com.banduairlines.repository;

import com.banduairlines.model.Boleto;
import com.banduairlines.model.Reserva;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import java.util.List;

public interface VuelosFidelizacionRepository extends Repository<Boleto, Long> {
    @Query("select r from Reserva r where r.usuario.id = :idUsuario and r.estado = com.banduairlines.model.enums.EstadoReserva.CONFIRMADA order by r.id")
    List<Reserva> reservasConfirmadas(Long idUsuario);

    @Query("select b from Boleto b join fetch b.pasajeroId join fetch b.vueloId v join fetch v.ruta where b.reserva.id = :idReserva")
    List<Boleto> boletos(Long idReserva);

}
