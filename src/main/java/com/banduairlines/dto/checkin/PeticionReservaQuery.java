package com.banduairlines.dto.checkin;
import com.fasterxml.jackson.annotation.JsonProperty;

public record PeticionReservaQuery (
    @JsonProperty("correo_electronico") String correoElectronico,
    @JsonProperty("codigo_pnr") String codigoPnr
){}
