package com.banduairlines.service.cuenta;

import com.banduairlines.dto.cuenta.RespuestaBeneficio;

import java.util.List;

public interface CatalogoBeneficioService {

    List<RespuestaBeneficio> listarActual();
}
