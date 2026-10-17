package com.banduairlines.repository;

import com.banduairlines.persistence.cuenta.RegistroPuntos;
import com.banduairlines.persistence.cuenta.TipoRegistroPuntos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Consultas de solo lectura sobre RegistroPuntos para la fidelización y los canjes.
 * El registro de movimientos y el cálculo del saldo están en RegistroPuntosRepository.
 */
public interface HistorialPuntosRepository extends Repository<RegistroPuntos, Long> {

    Page<RegistroPuntos> findByIdUsuarioOrderByFechaDescIdRegistroDesc(Long idUsuario, Pageable pagina);

    List<RegistroPuntos> findTop5ByIdUsuarioOrderByFechaDescIdRegistroDesc(Long idUsuario);

    Optional<RegistroPuntos> findFirstByIdUsuarioAndTipoAndReferenciaStartingWith(
            Long idUsuario, TipoRegistroPuntos tipo, String prefijoReferencia);

    /**
     * Saldo que tenía el usuario justo después del movimiento indicado (incluido).
     * Los movimientos de un mismo usuario se registran con la cuenta bloqueada, así que el orden
     * de los identificadores es el orden en que ocurrieron.
     */
    @Query("select coalesce(sum(case when r.tipo = com.banduairlines.persistence.cuenta.TipoRegistroPuntos.ACUMULACION"
            + " then r.cantidad else -r.cantidad end), 0)"
            + " from RegistroPuntos r where r.idUsuario = :idUsuario and r.idRegistro <= :idRegistro")
    long calcularSaldoHasta(@Param("idUsuario") Long idUsuario, @Param("idRegistro") Long idRegistro);
}
