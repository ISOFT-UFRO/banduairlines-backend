package com.banduairlines.service.impl;

import org.springframework.stereotype.Service;

import com.banduairlines.dto.busqueda.PasajerosBusquedaRequest;
import com.banduairlines.dto.busqueda.ResumenPasajerosResponse;
import com.banduairlines.service.busqueda.PasajerosBusquedaService;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasajerosBusquedaServiceImpl implements PasajerosBusquedaService {

    private final Validator validator;

    @Override
    public ResumenPasajerosResponse validarYResumir(PasajerosBusquedaRequest request) {
        var errores = validator.validate(request);
        if (!errores.isEmpty()) {
            throw new ConstraintViolationException(errores);
        }

        int adultos = request.getCantidadAdultos();
        int ninos = request.getCantidadNinos();
        int bebes = request.getCantidadBebes();
        long asientos = (long) adultos + ninos;
        return new ResumenPasajerosResponse(adultos, ninos, bebes, asientos + bebes, asientos);
    }
}
