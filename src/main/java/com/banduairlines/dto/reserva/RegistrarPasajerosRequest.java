package com.banduairlines.dto.reserva;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import com.banduairlines.model.enums.TipoDocumento;
import com.banduairlines.model.enums.TipoPasajero;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de PUT /reservas/{reservaId}/pasajeros (R-02): la lista completa de pasajeros.
 * ref la asigna el cliente para enlazar adultoResponsableRef dentro de la misma petición.
 * Diferencia con el contrato: no se pide nacionalidad porque el modelo Pasajero no tiene dónde guardarla.
 */
public record RegistrarPasajerosRequest(
        @NotEmpty(message = "la lista de pasajeros es obligatoria")
        List<@Valid @NotNull(message = "el pasajero no puede ser nulo") Pasajero> pasajeros
) {

    public record Pasajero(
            @NotBlank(message = "la ref es obligatoria")
            @Size(max = 40, message = "la ref no puede superar los 40 caracteres")
            String ref,

            @NotNull(message = "el tipo es obligatorio")
            TipoPasajero tipo,

            @NotBlank(message = "los nombres son obligatorios")
            @Size(max = 80, message = "los nombres no pueden superar los 80 caracteres")
            String nombres,

            @NotBlank(message = "los apellidos son obligatorios")
            @Size(max = 80, message = "los apellidos no pueden superar los 80 caracteres")
            String apellidos,

            @NotNull(message = "la fechaNacimiento es obligatoria")
            @Past(message = "la fechaNacimiento debe ser anterior a hoy")
            LocalDate fechaNacimiento,

            @NotNull(message = "el tipoDocumento es obligatorio")
            TipoDocumento tipoDocumento,

            /* "PEND-" está reservado para los pasajeros provisorios que crea POST /reservas. */
            @NotBlank(message = "el numeroDocumento es obligatorio")
            @Size(max = 30, message = "el numeroDocumento no puede superar los 30 caracteres")
            @Pattern(regexp = "^(?![Pp][Ee][Nn][Dd]-).*$", message = "el numeroDocumento no puede empezar con PEND-")
            String numeroDocumento,

            @Size(max = 40, message = "el adultoResponsableRef no puede superar los 40 caracteres")
            String adultoResponsableRef
    ) {
    }

    @AssertTrue(message = "cada pasajero debe tener una ref distinta")
    public boolean isRefsSinRepetir() {
        return sinRepetir(p -> p.ref() == null ? null : p.ref().trim());
    }

    @AssertTrue(message = "dos pasajeros no pueden tener el mismo numeroDocumento")
    public boolean isDocumentosSinRepetir() {
        return sinRepetir(p -> p.numeroDocumento() == null ? null : p.numeroDocumento().trim().toUpperCase(Locale.ROOT));
    }

    private boolean sinRepetir(Function<Pasajero, String> clave) {
        if (pasajeros == null) {
            return true;
        }
        List<String> claves = pasajeros.stream().filter(Objects::nonNull).map(clave).filter(Objects::nonNull).toList();
        return claves.size() == claves.stream().distinct().count();
    }
}
