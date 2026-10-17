package com.banduairlines.dto.admin;

import com.banduairlines.model.enums.ClaseCabina;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvionRequestDtoTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void prepararValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        fabrica.close();
    }

    @Test
    void aceptarAeronaveConAsientoValido() {
        assertThat(validador.validate(solicitudValida())).isEmpty();
    }

    @Test
    void permitirAsientosOmitidos() {
        AvionRequest solicitud = solicitudValida();
        solicitud.setAsientos(null);

        assertThat(validador.validate(solicitud)).isEmpty();
    }

    @Test
    void permitirListaDeAsientosVacia() {
        AvionRequest solicitud = solicitudValida();
        solicitud.setAsientos(List.of());

        assertThat(validador.validate(solicitud)).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void rechazarFilaInvalidaEnAsientoAnidado(Integer fila) {
        AvionRequest solicitud = solicitudValida();
        solicitud.getAsientos().get(0).setFila(fila);

        assertThat(validador.validate(solicitud))
                .extracting(error -> error.getPropertyPath().toString())
                .contains("asientos[0].fila");
    }

    @Test
    void rechazarLetraVaciaEnAsientoAnidado() {
        AvionRequest solicitud = solicitudValida();
        solicitud.getAsientos().get(0).setLetra("");

        assertThat(validador.validate(solicitud))
                .extracting(error -> error.getPropertyPath().toString())
                .contains("asientos[0].letra");
    }

    @Test
    void rechazarElementoNuloEnListaDeAsientos() {
        AvionRequest solicitud = solicitudValida();
        solicitud.setAsientos(Collections.singletonList(null));

        assertThat(validador.validate(solicitud)).isNotEmpty();
    }

    private AvionRequest solicitudValida() {
        AsientoRequest asiento = new AsientoRequest();
        asiento.setFila(1);
        asiento.setLetra("A");
        asiento.setClaseCabina(ClaseCabina.ECONOMY);
        asiento.setEsPreferente(false);

        AvionRequest solicitud = new AvionRequest();
        solicitud.setMatricula("CC-BNA");
        solicitud.setModelo("Airbus A320");
        solicitud.setAsientos(List.of(asiento));

        return solicitud;
    }
}