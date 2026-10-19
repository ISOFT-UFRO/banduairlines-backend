package com.banduairlines.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Aeropuerto;

public interface AeropuertoRepository extends JpaRepository<Aeropuerto, Long> {

    List<Aeropuerto> findAllByOrderByCiudadAscCodigoIataAsc();

    boolean existsByCodigoIata(String codigoIata);
}
