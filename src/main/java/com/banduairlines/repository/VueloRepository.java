package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.Estado;

import com.banduairlines.model.Vuelo;

public interface VueloRepository extends JpaRepository<Vuelo, Long>{
    
}
