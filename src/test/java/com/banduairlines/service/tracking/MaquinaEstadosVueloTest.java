package com.banduairlines.service.tracking;

import static com.banduairlines.model.enums.EstadoVuelo.ATERRIZADO;
import static com.banduairlines.model.enums.EstadoVuelo.CANCELADO;
import static com.banduairlines.model.enums.EstadoVuelo.DESPEGADO;
import static com.banduairlines.model.enums.EstadoVuelo.EMBARCANDO;
import static com.banduairlines.model.enums.EstadoVuelo.EN_VUELO;
import static com.banduairlines.model.enums.EstadoVuelo.PROGRAMADO;
import static com.banduairlines.model.enums.EstadoVuelo.RETRASADO;
import static com.banduairlines.model.enums.OrigenEvento.MANUAL;
import static com.banduairlines.model.enums.OrigenEvento.SISTEMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.OrigenEvento;

class MaquinaEstadosVueloTest {

    // Copia independiente de la tabla del README §5.1: el test no reutiliza la de la implementación.
    private static final Set<String> PERMITIDAS = Set.of(
            "PROGRAMADO>EMBARCANDO>SISTEMA", "PROGRAMADO>EMBARCANDO>MANUAL",
            "PROGRAMADO>RETRASADO>SISTEMA", "PROGRAMADO>RETRASADO>MANUAL",
            "PROGRAMADO>CANCELADO>SISTEMA", "PROGRAMADO>CANCELADO>MANUAL",
            "RETRASADO>EMBARCANDO>SISTEMA", "RETRASADO>EMBARCANDO>MANUAL",
            "RETRASADO>RETRASADO>SISTEMA", "RETRASADO>RETRASADO>MANUAL",
            "RETRASADO>CANCELADO>SISTEMA", "RETRASADO>CANCELADO>MANUAL",
            "EMBARCANDO>DESPEGADO>SISTEMA", "EMBARCANDO>DESPEGADO>MANUAL",
            "EMBARCANDO>RETRASADO>SISTEMA", "EMBARCANDO>RETRASADO>MANUAL",
            "EMBARCANDO>CANCELADO>SISTEMA", "EMBARCANDO>CANCELADO>MANUAL",
            "DESPEGADO>EN_VUELO>SISTEMA",
            "EN_VUELO>ATERRIZADO>SISTEMA"
    );

    static Stream<Arguments> todasLasCombinaciones() {
        List<Arguments> casos = new ArrayList<>();
        for (EstadoVuelo actual : EstadoVuelo.values()) {
            for (EstadoVuelo nuevo : EstadoVuelo.values()) {
                for (OrigenEvento origen : OrigenEvento.values()) {
                    casos.add(Arguments.of(actual, nuevo, origen));
                }
            }
        }
        return casos.stream();
    }

    @ParameterizedTest(name = "{0} -> {1} ({2})")
    @MethodSource("todasLasCombinaciones")
    void permite_coincideConLaTablaEn7x7x2(EstadoVuelo actual, EstadoVuelo nuevo, OrigenEvento origen) {
        boolean esperado = PERMITIDAS.contains(actual + ">" + nuevo + ">" + origen);

        assertThat(MaquinaEstadosVuelo.permite(actual, nuevo, origen)).isEqualTo(esperado);

        if (esperado) {
            assertThatCode(() -> MaquinaEstadosVuelo.validar(actual, nuevo, origen, "motivo"))
                    .doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> MaquinaEstadosVuelo.validar(actual, nuevo, origen, "motivo"))
                    .isInstanceOf(ReglaNegocioException.class)
                    .extracting("codigo").isEqualTo(CodigosTracking.TRANSICION_ESTADO_INVALIDA);
        }
    }

    @ParameterizedTest
    @EnumSource(EstadoVuelo.class)
    void terminales_rechazanCualquierDestino(EstadoVuelo nuevo) {
        for (OrigenEvento origen : OrigenEvento.values()) {
            assertThat(MaquinaEstadosVuelo.permite(ATERRIZADO, nuevo, origen)).isFalse();
            assertThat(MaquinaEstadosVuelo.permite(CANCELADO, nuevo, origen)).isFalse();
        }
    }

    @Test
    void esTerminal_soloAterrizadoYCancelado() {
        assertThat(Stream.of(EstadoVuelo.values()).filter(MaquinaEstadosVuelo::esTerminal))
                .containsExactlyInAnyOrder(ATERRIZADO, CANCELADO);
    }

    @Test
    void validar_cuandoAdministradorMarcaEnVuelo_lanzaTransicionInvalida() {
        assertThatThrownBy(() -> MaquinaEstadosVuelo.validar(DESPEGADO, EN_VUELO, MANUAL, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("solo la produce el sistema")
                .extracting("codigo").isEqualTo(CodigosTracking.TRANSICION_ESTADO_INVALIDA);
    }

    @Test
    void validar_cuandoCanceladoSinMotivo_lanzaMotivoRequerido() {
        assertThatThrownBy(() -> MaquinaEstadosVuelo.validar(PROGRAMADO, CANCELADO, MANUAL, null))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigosTracking.MOTIVO_REQUERIDO);
        assertThatThrownBy(() -> MaquinaEstadosVuelo.validar(PROGRAMADO, RETRASADO, SISTEMA, "   "))
                .extracting("codigo").isEqualTo(CodigosTracking.MOTIVO_REQUERIDO);
    }

    @Test
    void validar_cuandoTransicionInvalidaYSinMotivo_priorizaTransicionInvalida() {
        assertThatThrownBy(() -> MaquinaEstadosVuelo.validar(ATERRIZADO, CANCELADO, MANUAL, null))
                .extracting("codigo").isEqualTo(CodigosTracking.TRANSICION_ESTADO_INVALIDA);
    }

    @Test
    void validar_cuandoEstadoNoRequiereMotivo_aceptaSinMotivo() {
        assertThatCode(() -> MaquinaEstadosVuelo.validar(PROGRAMADO, EMBARCANDO, MANUAL, null))
                .doesNotThrowAnyException();
    }
}
