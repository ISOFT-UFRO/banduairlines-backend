package com.banduairlines.service.cuenta;

import com.banduairlines.dto.cuenta.RespuestaPaginada;
import com.banduairlines.dto.cuenta.RespuestaUsuario;
import com.banduairlines.dto.cuenta.SolicitudCreacionAdministrador;

public interface AdministracionCuentaService {

    RespuestaPaginada<RespuestaUsuario> listarAdministradores(int pagina, int tamano);

    RespuestaUsuario crearAdministrador(SolicitudCreacionAdministrador solicitud);

    RespuestaUsuario obtenerAdministrador(Long idAdministrador);

    void eliminarAdministrador(Long idAdministrador);
}
