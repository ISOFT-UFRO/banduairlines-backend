package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Reserva;
import com.banduairlines.model.TarjetaEmbarque;
import java.util.List;


public interface TarjetaEmbarqueRepository extends JpaRepository<TarjetaEmbarque, Long>{
    // Tarjetas de todos los boletos de la reserva (tarjeta.boleto.reserva)
    List<TarjetaEmbarque> findByBoletoReserva(Reserva reserva);
}
