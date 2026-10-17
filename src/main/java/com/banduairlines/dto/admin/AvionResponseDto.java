package com.banduairlines.dto.admin;

import com.banduairlines.model.enums.Estado;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AvionResponseDto {
    private Long id;
    private String matricula;
    private String modelo;
    private Estado estado;
    private int totalAsientos;
}
