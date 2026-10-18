package com.banduairlines.service.cuenta;

import com.banduairlines.dto.cuenta.RespuestaSesion;
import com.banduairlines.dto.cuenta.SolicitudInicioSesion;

public interface InicioSesionService {

    RespuestaSesion iniciarSesion(SolicitudInicioSesion solicitud);
}
