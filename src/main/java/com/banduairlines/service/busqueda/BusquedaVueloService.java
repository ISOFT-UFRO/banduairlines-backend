package com.banduairlines.service.busqueda;

import com.banduairlines.dto.busqueda.ClaseCabinaDisponibilidadResponse;
import com.banduairlines.dto.busqueda.ItinerarioVueloResponse;
import com.banduairlines.dto.busqueda.ProgramacionVueloRequest;
import com.banduairlines.dto.busqueda.ProgramacionVueloResponse;

import java.time.LocalDate;
import java.util.List;

public interface BusquedaVueloService {

    List<ItinerarioVueloResponse> obtenerAlternativasItinerario(
            String origenIata,
            String destinoIata,
            LocalDate fechaSalida
    );

    List<ClaseCabinaDisponibilidadResponse> obtenerClasesCabinaDisponibles(Long vueloId);

    List<ClaseCabinaDisponibilidadResponse> obtenerClasesCabinaDisponibles(Long vueloId, long asientosNecesarios);

    ProgramacionVueloResponse programarVuelo(ProgramacionVueloRequest request);

    ProgramacionVueloResponse modificarProgramacionVuelo(Long vueloId, ProgramacionVueloRequest request);
}