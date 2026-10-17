package com.banduairlines.repository;

import com.banduairlines.model.Pasajero;
import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Boleto;

import java.util.List;

public interface BoletoRepository extends JpaRepository<Boleto, Long>{
    List<Boleto> findByReserva_IdOrderByOrdenTramoAscIdAsc(Long reservaId);

    List<Boleto> findByPasajeroId(Pasajero pasajeroId);
}
