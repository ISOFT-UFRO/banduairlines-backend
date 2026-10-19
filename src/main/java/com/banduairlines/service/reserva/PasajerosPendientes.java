package com.banduairlines.service.reserva;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.TipoDocumento;
import com.banduairlines.model.enums.TipoPasajero;

/**
 * Pasajeros provisorios que crea POST /reservas para guardar los tramos elegidos en boletos sin modificar el modelo
 * (Boleto exige un pasajero). PUT /pasajeros los reemplaza por los datos reales en los mismos registros
 * Se reconocen por el numero de documento
 */
@Component
public class PasajerosPendientes {

    public static final String PREFIJO_DOCUMENTO = "PEND-";
    public static final String NOMBRE = "PENDIENTE";

    public Pasajero crear(Reserva reserva, TipoPasajero tipo, int numero, LocalDate fechaPrimerVuelo) {
        return Pasajero.builder()
                .reserva(reserva)
                .tipo(tipo)
                .nombre(NOMBRE)
                .apellido(NOMBRE)
                .tipoDocumento(TipoDocumento.OTRO)
                .numeroDocumento(PREFIJO_DOCUMENTO + numero)
                .fechaNacimiento(fechaCoherente(tipo, fechaPrimerVuelo))
                .build();
    }

    public static boolean esPendiente(Pasajero pasajero) {
        return pasajero.getTipoDocumento() == TipoDocumento.OTRO
                && pasajero.getNumeroDocumento() != null
                && pasajero.getNumeroDocumento().startsWith(PREFIJO_DOCUMENTO);
    }

    /** La columna fecha_nacimiento es obligatoria; se usa una fecha que calza con el tipo declarado*/
    private static LocalDate fechaCoherente(TipoPasajero tipo, LocalDate fechaPrimerVuelo) {
        return switch (tipo) {
            case ADULTO -> fechaPrimerVuelo.minusYears(30);
            case NINO -> fechaPrimerVuelo.minusYears(6);
            case BEBE -> fechaPrimerVuelo.minusMonths(6);
        };
    }
}
