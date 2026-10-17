package com.banduairlines.dto.admin;

public record EquipajePackResponse(
    Long packId,
    String nombrePack,
    Boolean incluyeEquipajeCabina,
    Integer piezasBodegaIncluidas,
    Integer kgPorPiezaBodega
) {
}