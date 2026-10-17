package com.banduairlines.service.tracking.puerto;

import java.util.List;
import java.util.Optional;

// Lectura de Reserva/Boleto del módulo RESERVA para las consultas de RF-04 (RNF-01: no lee de Búsqueda).
public interface ReservaExternaPort {

    Optional<ReservaExterna> buscarPorCodigo(String codigoReserva);

    List<Long> vueloIdsDeUsuario(Long usuarioId);

    List<Long> vueloIdsDeReserva(Long reservaId);

    boolean documentoPerteneceAReserva(Long reservaId, String numeroDocumento);
}
