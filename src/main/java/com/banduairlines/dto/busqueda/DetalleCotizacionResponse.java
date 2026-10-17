package com.banduairlines.dto.busqueda;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DetalleCotizacionResponse {

    private Long vueloId;

    private Long tarifaId;

    private String claseCabina;

    private BigDecimal subtotalAdultos;

    private BigDecimal subtotalNinos;

    private BigDecimal subtotalBebes;

    private BigDecimal impuestos;

    private BigDecimal precioTotal;

    private BigDecimal precioPorPasajero;

    private String moneda;

    private ResumenPasajerosResponse pasajeros;

    private PackTarifarioBusquedaResponse pack;

    private List<TramoTarifaResponse> tramos;

    private SeleccionReservaResponse seleccionReserva;
}
