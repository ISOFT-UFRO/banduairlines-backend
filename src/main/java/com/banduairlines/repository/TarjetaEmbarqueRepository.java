package com.banduairlines.repository;

import com.banduairlines.model.Boleto;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Vuelo;
import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.TarjetaEmbarque;

import java.util.List;
import java.util.Optional;

public interface TarjetaEmbarqueRepository extends JpaRepository<TarjetaEmbarque, Long>{
    Optional<TarjetaEmbarque> findByBoleto(Boleto boleto);
    List<TarjetaEmbarque> findByBoletoReservaId(Reserva reserva);
    List<TarjetaEmbarque> findByBoletoVueloId(Vuelo vuelo);
}
