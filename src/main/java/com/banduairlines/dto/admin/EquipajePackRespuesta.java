package com.banduairlines.dto.admin;

public record EquipajePackRespuesta(
    Long packId,
    String nombrePack,
    Boolean incluyeEquipajeCabina,
    Integer piezasBodegaIncluidas,
    Integer kgPorPiezaBodega
) {
}