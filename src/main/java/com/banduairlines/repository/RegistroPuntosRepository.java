package com.banduairlines.repository;

import com.banduairlines.persistence.cuenta.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import java.util.Optional;

public interface RegistroPuntosRepository extends Repository<RegistroPuntos, Long> {
    RegistroPuntos save(RegistroPuntos registro);
    java.util.List<RegistroPuntos> findByIdUsuarioAndTipo(Long idUsuario, TipoRegistroPuntos tipo);
    Optional<RegistroPuntos> findByIdUsuarioAndTipoAndReferencia(Long idUsuario, TipoRegistroPuntos tipo, String referencia);
    @Query("select coalesce(sum(case when r.tipo = com.banduairlines.persistence.cuenta.TipoRegistroPuntos.ACUMULACION then r.cantidad else -r.cantidad end), 0) from RegistroPuntos r where r.idUsuario = :idUsuario")
    long calcularSaldo(Long idUsuario);
}
