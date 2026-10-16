package com.banduairlines.repository;

import com.banduairlines.persistence.cuenta.RegistroPuntos;
import com.banduairlines.persistence.cuenta.TipoRegistroPuntos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

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
}
