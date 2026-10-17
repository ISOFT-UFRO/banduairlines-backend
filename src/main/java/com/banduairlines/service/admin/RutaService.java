package com.banduairlines.service.admin;

import org.springframework.data.domain.Page;

import com.banduairlines.dto.admin.RutaResponse;
import com.banduairlines.dto.admin.RutaRequest;
import java.util.List;

public interface RutaService {
    Page<RutaResponse> listar(
            Boolean activa,
            int pagina,
            int tamaño);

    Page<RutaResponse> buscar(
            Boolean activa,
            String texto,
            int pagina,
            int tamano);

    RutaResponse obtener(Long id);
    RutaResponse crear(RutaRequest solicitud);
    RutaResponse actualizar(Long id, RutaRequest solicitud);
    RutaResponse cambiarEstado(Long id, boolean activa);
    List<RutaResponse> listarTodas(Boolean activa);
    List<RutaResponse> buscarTodas(Boolean activa, String texto);

}
