package com.banduairlines.service.cuenta;

import com.banduairlines.dto.cuenta.RespuestaSesion;
import com.banduairlines.dto.cuenta.SolicitudRenovacion;

public interface RenovacionSesionService {

    RespuestaSesion renovarSesion(SolicitudRenovacion solicitud);
}
