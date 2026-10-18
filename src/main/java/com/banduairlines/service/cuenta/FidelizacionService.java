package com.banduairlines.service.cuenta;

import com.banduairlines.dto.cuenta.RespuestaFidelizacion;
import com.banduairlines.dto.cuenta.RespuestaMovimientoPuntos;
import com.banduairlines.dto.cuenta.RespuestaPaginada;

public interface FidelizacionService {

    RespuestaFidelizacion consultarActual();

    RespuestaPaginada<RespuestaMovimientoPuntos> listarMovimientosActual(int pagina, int tamano);
}
