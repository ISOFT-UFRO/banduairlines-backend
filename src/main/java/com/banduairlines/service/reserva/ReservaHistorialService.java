package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.PaginaReservasResponse;
import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.model.enums.EstadoReserva;

//endpoints 14 (buscar con código y correo) y 15 (historial del pasajero autenticado), falta implementar
public interface ReservaHistorialService {

    ReservaResponse buscar(String codigo, String correo, String clienteIp);

    PaginaReservasResponse listarMias(EstadoReserva estado, int pagina, int tamano);
}
