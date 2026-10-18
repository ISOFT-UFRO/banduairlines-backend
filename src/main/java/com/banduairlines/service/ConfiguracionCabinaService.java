package com.banduairlines.service;

import com.banduairlines.dto.busqueda.ConfiguracionCabinasResponse;
import com.banduairlines.dto.busqueda.ConfigurarCabinasRequest;

public interface ConfiguracionCabinaService {

    // --- Métodos ---

    ConfiguracionCabinasResponse configurarCabinas(Long vueloId, ConfigurarCabinasRequest request);

}