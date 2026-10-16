package com.banduairlines.service.reportes;

import com.banduairlines.dto.reportes.ReporteResponse;

import java.time.LocalDate;

public interface ReporteService {

    ReporteResponse obtenerPorFecha(LocalDate fecha);
}
    //Obtenr datos del modulo Pago
        // estado del pago
        // cantidad pagado y confirmado
        // transsacciónes exitosas
        //monto netos cobrados
        //fecha en la que se liquida
    //Obtener datos del modulo Reserva
        //clase de la cabina
        //tipo de pasajero
        //codigo de reserva
        //fecha de reserva
        //estado pendente, confirmada, cancelada
    //obtener datos de administración
        //Catalogo de vuelos
        //rutas de origen-destino
        //capacidad de acientos por avión
