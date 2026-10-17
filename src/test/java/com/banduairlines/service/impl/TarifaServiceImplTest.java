package com.banduairlines.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.banduairlines.dto.admin.TarifaRequest;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Asiento;
import com.banduairlines.model.Avion;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.repository.AsientoRepository;
import com.banduairlines.repository.PackTarifarioRepository;
import com.banduairlines.repository.TarifaVueloRepository;
import com.banduairlines.repository.VueloRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TarifaServiceImplTest {

    @Mock
    private TarifaVueloRepository tarifas;

    @Mock
    private VueloRepository vuelos;

    @Mock
    private PackTarifarioRepository packs;

    @Mock
    private AsientoRepository asientos;

    @InjectMocks
    private TarifaServiceImpl servicio;

    @ParameterizedTest
    @ValueSource(strings = { "0", "45990", "99999999" })
    void crearAceptaPreciosValidosYConservaLaCombinacion(String valor) {
        Vuelo vuelo = vuelo();
        PackTarifario pack = pack(true);
        BigDecimal precio = new BigDecimal(valor);

        when(vuelos.findById(1L)).thenReturn(Optional.of(vuelo));
        when(packs.findById(2L)).thenReturn(Optional.of(pack));
        prepararCabina();

        when(tarifas.saveAndFlush(any(TarifaVuelo.class)))
                .thenAnswer(invocacion -> {
                    TarifaVuelo tarifa = invocacion.getArgument(0);
                    tarifa.setId(10L);
                    return tarifa;
                });

        var respuesta = servicio.crear(1L, solicitud(precio));

        assertThat(respuesta.id()).isEqualTo(10L);
        assertThat(respuesta.vueloId()).isEqualTo(1L);
        assertThat(respuesta.claseCabina()).isEqualTo(ClaseCabina.ECONOMY);
        assertThat(respuesta.packId()).isEqualTo(2L);
        assertThat(respuesta.nombrePack()).isEqualTo("Full");
        assertThat(respuesta.packActivo()).isTrue();
        assertThat(respuesta.precio()).isEqualByComparingTo(precio);
        assertThat(respuesta.moneda()).isEqualTo("CLP");

        verify(tarifas).contarDuplicadasAdministracion(
                1L, ClaseCabina.ECONOMY, 2L);
        verify(tarifas).saveAndFlush(any(TarifaVuelo.class));
    }

    @Test
    void crearRechazaCombinacionDuplicadaSinGuardar() {
        when(vuelos.findById(1L)).thenReturn(Optional.of(vuelo()));
        when(packs.findById(2L)).thenReturn(Optional.of(pack(true)));
        prepararCabina();

        when(tarifas.contarDuplicadasAdministracion(
                1L, ClaseCabina.ECONOMY, 2L)).thenReturn(1L);

        assertThatThrownBy(() -> servicio.crear(1L, solicitud(new BigDecimal("45990"))))
                .isInstanceOfSatisfying(
                        ConflictoException.class,
                        excepcion -> assertThat(excepcion.getCodigo())
                                .isEqualTo("TARIFA_DUPLICADA"));

        verify(tarifas, never()).saveAndFlush(any(TarifaVuelo.class));
    }

    @Test
    void crearRechazaPackInactivoSinGuardar() {
        when(vuelos.findById(1L)).thenReturn(Optional.of(vuelo()));
        when(packs.findById(2L)).thenReturn(Optional.of(pack(false)));

        assertThatThrownBy(() -> servicio.crear(1L, solicitud(new BigDecimal("45990"))))
                .isInstanceOfSatisfying(
                        ReglaNegocioException.class,
                        excepcion -> assertThat(excepcion.getCodigo())
                                .isEqualTo("PACK_INACTIVO"));

        verify(tarifas, never()).saveAndFlush(any(TarifaVuelo.class));
    }

    @Test
    void crearRechazaCabinaSinAsientosEnLaAeronave() {
        when(vuelos.findById(1L)).thenReturn(Optional.of(vuelo()));
        when(packs.findById(2L)).thenReturn(Optional.of(pack(true)));

        when(asientos.findByAeronaveIdAndClaseCabina(
                3L, ClaseCabina.ECONOMY)).thenReturn(List.of());

        assertThatThrownBy(() -> servicio.crear(1L, solicitud(new BigDecimal("45990"))))
                .isInstanceOfSatisfying(
                        ReglaNegocioException.class,
                        excepcion -> assertThat(excepcion.getCodigo())
                                .isEqualTo("CABINA_NO_DISPONIBLE_EN_AERONAVE"));

        verify(tarifas, never()).saveAndFlush(any(TarifaVuelo.class));
    }

    @Test
    void crearRechazaPackInexistente() {
        when(vuelos.findById(1L)).thenReturn(Optional.of(vuelo()));
        when(packs.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(1L, solicitud(new BigDecimal("45990"))))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Pack tarifario no encontrado: 2");

        verify(tarifas, never()).saveAndFlush(any(TarifaVuelo.class));
    }

    @Test
    void crearRechazaVueloInexistente() {
        when(vuelos.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(1L, solicitud(new BigDecimal("45990"))))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Vuelo no encontrado: 1");

        verify(tarifas, never()).saveAndFlush(any(TarifaVuelo.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void actualizarModificaSoloPrecioInclusoConPackInactivo(
            boolean packActivo) {

        TarifaVuelo tarifa = tarifa(packActivo);
        Vuelo vueloOriginal = tarifa.getVuelo();
        PackTarifario packOriginal = tarifa.getPack();

        when(vuelos.findById(1L))
                .thenReturn(Optional.of(vueloOriginal));
        when(tarifas.buscarAdministracion(1L, 10L))
                .thenReturn(Optional.of(tarifa));
        when(tarifas.saveAndFlush(tarifa)).thenReturn(tarifa);
        prepararCabina();

        var respuesta = servicio.actualizarPrecio(
                1L, 10L, new BigDecimal("49990"));

        assertThat(respuesta.precio()).isEqualByComparingTo("49990");
        assertThat(respuesta.packActivo()).isEqualTo(packActivo);
        assertThat(tarifa.getId()).isEqualTo(10L);
        assertThat(tarifa.getVuelo()).isSameAs(vueloOriginal);
        assertThat(tarifa.getPack()).isSameAs(packOriginal);
        assertThat(tarifa.getClaseCabina()).isEqualTo(ClaseCabina.ECONOMY);

        verify(tarifas).saveAndFlush(tarifa);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "-1", "0.50", "100000000" })
    void actualizarRechazaPrecioInvalidoSinModificarLaTarifa(String valor) {
        TarifaVuelo tarifa = tarifa(true);

        when(vuelos.findById(1L))
                .thenReturn(Optional.of(tarifa.getVuelo()));
        when(tarifas.buscarAdministracion(1L, 10L))
                .thenReturn(Optional.of(tarifa));

        BigDecimal precio = valor == null ? null : new BigDecimal(valor);

        assertThatThrownBy(() -> servicio.actualizarPrecio(1L, 10L, precio))
                .isInstanceOfSatisfying(
                        ReglaNegocioException.class,
                        excepcion -> assertThat(excepcion.getCodigo())
                                .isEqualTo("PRECIO_TARIFA_INVALIDO"));

        assertThat(tarifa.getPrecio()).isEqualByComparingTo("45990");
        verify(tarifas, never()).saveAndFlush(any(TarifaVuelo.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "obtener", "actualizarPrecio" })
    void rechazaTarifaQueNoExisteDentroDelVuelo(String operacion) {
        when(vuelos.findById(1L)).thenReturn(Optional.of(vuelo()));
        when(tarifas.buscarAdministracion(1L, 99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> {
            if ("obtener".equals(operacion)) {
                servicio.obtener(1L, 99L);
            } else {
                servicio.actualizarPrecio(
                        1L, 99L, new BigDecimal("49990"));
            }
        })
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Tarifa no encontrado: 99");

        verify(tarifas).buscarAdministracion(1L, 99L);
        verify(tarifas, never()).saveAndFlush(any(TarifaVuelo.class));
    }

    @Test
    void actualizarRechazaCabinaQueYaNoExisteSinCambiarPrecio() {
        TarifaVuelo tarifa = tarifa(true);

        when(vuelos.findById(1L))
                .thenReturn(Optional.of(tarifa.getVuelo()));
        when(tarifas.buscarAdministracion(1L, 10L))
                .thenReturn(Optional.of(tarifa));
        when(asientos.findByAeronaveIdAndClaseCabina(
                3L, ClaseCabina.ECONOMY)).thenReturn(List.of());

        assertThatThrownBy(() -> servicio.actualizarPrecio(
                1L, 10L, new BigDecimal("49990")))
                .isInstanceOfSatisfying(
                        ReglaNegocioException.class,
                        excepcion -> assertThat(excepcion.getCodigo())
                                .isEqualTo("CABINA_NO_DISPONIBLE_EN_AERONAVE"));

        assertThat(tarifa.getPrecio()).isEqualByComparingTo("45990");
        verify(tarifas, never()).saveAndFlush(any(TarifaVuelo.class));
    }

    private void prepararCabina() {
        when(asientos.findByAeronaveIdAndClaseCabina(
                3L, ClaseCabina.ECONOMY))
                .thenReturn(List.of(new Asiento()));
    }

    private TarifaRequest solicitud(BigDecimal precio) {
        return new TarifaRequest(ClaseCabina.ECONOMY, 2L, precio);
    }

    private Vuelo vuelo() {
        return Vuelo.builder()
                .id(1L)
                .aeronave(Avion.builder().id(3L).build())
                .build();
    }

    private PackTarifario pack(boolean activo) {
        return PackTarifario.builder()
                .id(2L)
                .nombre("Full")
                .activo(activo)
                .build();
    }

    private TarifaVuelo tarifa(boolean packActivo) {
        return TarifaVuelo.builder()
                .id(10L)
                .vuelo(vuelo())
                .claseCabina(ClaseCabina.ECONOMY)
                .pack(pack(packActivo))
                .precio(new BigDecimal("45990"))
                .build();
    }
}