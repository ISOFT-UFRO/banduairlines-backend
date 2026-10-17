package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.banduairlines.model.enums.TipoMovimiento;
import java.util.List;
import java.util.Optional;

import com.banduairlines.model.MovimientoMilla;

public interface MovimientoMillaRepository extends JpaRepository<MovimientoMilla, Long>{
    List<MovimientoMilla> findByCuentaFidelizacion_UsuarioIdAndTipo(Long idUsuario, TipoMovimiento tipo);

    Optional<MovimientoMilla> findByCuentaFidelizacion_UsuarioIdAndTipoAndDescripcion(
            Long idUsuario, TipoMovimiento tipo, String descripcion);

    @Query("select coalesce(sum(m.millas), 0) from MovimientoMilla m where m.cuentaFidelizacion.usuarioId = :idUsuario")
    long calcularSaldo(Long idUsuario);
}
