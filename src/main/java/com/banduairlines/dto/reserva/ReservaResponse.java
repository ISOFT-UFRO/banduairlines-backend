package com.banduairlines.dto.reserva;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.Sentido;

/**
 * Reserva completa: respuesta de GET /reservas/{reservaId} (R-05, R-07) y de POST /reservas/{reservaId}/cancelacion.
 * pasajerosRegistrados es false mientras no se haya hecho PUT /pasajeros (la lista de pasajeros viene vacía).
 */
public record ReservaResponse(
        Long reservaId,
        String codigoReserva,
        EstadoReserva estado,
        OffsetDateTime expiraEn,
        String pack,
        CantidadPasajeros cantidadPasajeros,
        List<Tramo> itinerario,
        boolean pasajerosRegistrados,
        List<PasajeroResponse> pasajeros,
        List<Asignacion> asientos,
        List<Servicio> servicios,
        Promocion promocion,
        ContactoResponse contacto,
        TotalResponse total
) {

    public record CantidadPasajeros(int adultos, int ninos, int bebes) {
    }

    /** salida y llegada en hora local del aeropuerto, como las guarda ADMIN. */
    public record Tramo(
            Long tramoId,
            int orden,
            Sentido sentido,
            String codigoVuelo,
            String origen,
            String destino,
            LocalDateTime salidaProgramada,
            LocalDateTime llegadaProgramada,
            ClaseCabina cabina,
            EstadoVuelo estadoVuelo
    ) {
    }

    /** asiento es null si el pasajero aún no lo elige. */
    public record Asignacion(Long pasajeroId, Long tramoId, String asiento) {
    }

    public record Servicio(Long servicioId, Long pasajeroId, Long tramoId, String nombre, int cantidad, long subtotalCLP) {
    }

    public record Promocion(String codigo, String tipoDescuento, long valor, OffsetDateTime vigenteHasta) {
    }
}
