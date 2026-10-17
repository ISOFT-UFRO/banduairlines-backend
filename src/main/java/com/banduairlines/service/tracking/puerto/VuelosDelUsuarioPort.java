package com.banduairlines.service.tracking.puerto;

import java.util.List;

// Vuelos asociados a un usuario para la consulta de RF-04 (RNF-01: no lee de Búsqueda).
public interface VuelosDelUsuarioPort {

    List<Long> vueloIdsDeUsuario(Long usuarioId);
}
