package com.banduairlines.dto.busqueda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionCabinasResponse {

    // --- Atributos ---

    private Long vueloId;
    private String mensaje;
    private List<CapacidadCabinaItemDTO> cabinas;

}