package com.banduairlines.repository;

import com.banduairlines.persistence.cuenta.SolicitudCambioCorreoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface SolicitudCambioCorreoRepository
        extends JpaRepository<SolicitudCambioCorreoEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SolicitudCambioCorreoEntity> findByIdSolicitudAndUsuario_Id(
            Long idSolicitud,
            Long idUsuario
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SolicitudCambioCorreoEntity>
    findByUsuario_IdAndFechaConfirmacionIsNullAndFechaCancelacionIsNull(
            Long idUsuario
    );
}
