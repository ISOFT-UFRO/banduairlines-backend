package com.banduairlines.dto.busqueda;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigurarCabinasRequest {

    // --- Atributos ---

    @NotEmpty(message = "La lista de configuración de cabinas no puede estar vacía")
    @Valid
    private List<CapacidadCabinaItemDTO> cabinas;
}