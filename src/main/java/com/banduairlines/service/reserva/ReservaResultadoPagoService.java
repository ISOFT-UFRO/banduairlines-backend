package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.ResultadoPagoRequest;
import com.banduairlines.dto.reserva.ResultadoPagoResponse;

//endpoint 13: recibe de PAGOS el resultado del cobro y confirma o mantiene la reserva.
//La autenticación por X-Service-Key la hace el controlador; este método sirve también para una llamada en proceso.
public interface ReservaResultadoPagoService {

    ResultadoPagoResponse registrar(Long reservaId, ResultadoPagoRequest request);
}