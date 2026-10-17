package com.banduairlines.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Boleto;
import com.banduairlines.model.Reserva;

public interface BoletoRepository extends JpaRepository<Boleto, Long>{

    List<Boleto> findByReserva(Reserva reserva);
    
}
