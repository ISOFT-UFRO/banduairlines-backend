package com.banduairlines.service.impl;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.admin.PackRequest;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.repository.PackTarifarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PackServiceImplTest {

    @Mock
    private PackTarifarioRepository packs;

    @InjectMocks
    private PackServiceImpl servicio;

    @Test
    void crearNormalizaNombreEInicializaActivoSinEquipaje() {
        when(packs.saveAndFlush(any(PackTarifario.class)))
                .thenAnswer(invocacion -> {
                    PackTarifario pack = invocacion.getArgument(0);
                    pack.setId(1L);
                    return pack;
                });

        var respuesta = servicio.crear(solicitud("  Full  "));

        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.nombre()).isEqualTo("Full");
        assertThat(respuesta.descripcion()).isEqualTo("Beneficios del pack");
        assertThat(respuesta.activo()).isTrue();
        assertThat(respuesta.incluyeEquipajeCabina()).isFalse();
        assertThat(respuesta.piezasBodegaIncluidas()).isZero();
        assertThat(respuesta.kgPorPiezaBodega()).isZero();
        assertThat(respuesta.incluyeSeleccionAsiento()).isTrue();
        assertThat(respuesta.incluyeEmbarquePrioritario()).isTrue();
        assertThat(respuesta.permiteCambios()).isTrue();
        assertThat(respuesta.multiplicadorMillas())
                .isEqualByComparingTo("1.50");

        verify(packs).contarDuplicadosAdministracion("  Full  ", null);
        verify(packs).saveAndFlush(any(PackTarifario.class));
    }

    @Test
    void crearRechazaNombreDuplicadoSinGuardar() {
        when(packs.contarDuplicadosAdministracion("Full", null))
                .thenReturn(1L);

        assertThatThrownBy(() -> servicio.crear(solicitud("Full")))
                .isInstanceOfSatisfying(
                        ConflictoException.class,
                        excepcion -> assertThat(excepcion.getCodigo())
                                .isEqualTo("PACK_DUPLICADO"));

        verify(packs, never()).saveAndFlush(any(PackTarifario.class));
    }

    @Test
    void actualizarConservaEquipajeYEstadoDelPack() {
        PackTarifario pack = packExistente();

        when(packs.findById(1L)).thenReturn(Optional.of(pack));
        when(packs.saveAndFlush(pack)).thenReturn(pack);

        var respuesta = servicio.actualizar(
                1L, solicitud("  Full actualizado  "));

        assertThat(respuesta.nombre()).isEqualTo("Full actualizado");
        assertThat(respuesta.descripcion()).isEqualTo("Beneficios del pack");
        assertThat(respuesta.incluyeSeleccionAsiento()).isTrue();
        assertThat(respuesta.incluyeEmbarquePrioritario()).isTrue();
        assertThat(respuesta.permiteCambios()).isTrue();
        assertThat(respuesta.multiplicadorMillas())
                .isEqualByComparingTo("1.50");

        assertThat(respuesta.incluyeEquipajeCabina()).isTrue();
        assertThat(respuesta.piezasBodegaIncluidas()).isEqualTo(2);
        assertThat(respuesta.kgPorPiezaBodega()).isEqualTo(23);
        assertThat(respuesta.activo()).isFalse();

        verify(packs).contarDuplicadosAdministracion(
                "  Full actualizado  ", 1L);
        verify(packs).saveAndFlush(pack);
    }

    @Test
    void actualizarRechazaNombreDuplicadoSinModificarElPack() {
        PackTarifario pack = packExistente();

        when(packs.findById(1L)).thenReturn(Optional.of(pack));
        when(packs.contarDuplicadosAdministracion("Light", 1L))
                .thenReturn(1L);

        assertThatThrownBy(() -> servicio.actualizar(1L, solicitud("Light")))
                .isInstanceOfSatisfying(
                        ConflictoException.class,
                        excepcion -> assertThat(excepcion.getCodigo())
                                .isEqualTo("PACK_DUPLICADO"));

        assertThat(pack.getNombre()).isEqualTo("Full");
        assertThat(pack.getDescripcion()).isEqualTo("Configuración anterior");
        assertThat(pack.getIncluyeSeleccionAsiento()).isFalse();
        assertThat(pack.getIncluyeEmbarquePrioritario()).isFalse();
        assertThat(pack.getPermiteCambios()).isFalse();
        assertThat(pack.getMultiplicadorMillas())
                .isEqualByComparingTo("1.00");
        assertThat(pack.getPiezasBodegaIncluidas()).isEqualTo(2);
        assertThat(pack.getActivo()).isFalse();

        verify(packs, never()).saveAndFlush(any(PackTarifario.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void cambiarEstadoConservaEquipajeYBeneficios(boolean activo) {
        PackTarifario pack = packExistente();
        pack.setActivo(!activo);

        when(packs.findById(1L)).thenReturn(Optional.of(pack));
        when(packs.saveAndFlush(pack)).thenReturn(pack);

        var respuesta = servicio.cambiarEstado(1L, activo);

        assertThat(respuesta.activo()).isEqualTo(activo);
        assertThat(respuesta.nombre()).isEqualTo("Full");
        assertThat(respuesta.incluyeEquipajeCabina()).isTrue();
        assertThat(respuesta.piezasBodegaIncluidas()).isEqualTo(2);
        assertThat(respuesta.kgPorPiezaBodega()).isEqualTo(23);
        assertThat(respuesta.incluyeSeleccionAsiento()).isFalse();
        assertThat(respuesta.incluyeEmbarquePrioritario()).isFalse();
        assertThat(respuesta.permiteCambios()).isFalse();
        assertThat(respuesta.multiplicadorMillas())
                .isEqualByComparingTo("1.00");

        verify(packs).saveAndFlush(pack);
    }

    @ParameterizedTest
    @ValueSource(strings = { "obtener", "actualizar", "cambiarEstado" })
    void rechazaPackInexistenteSinGuardar(String operacion) {
        when(packs.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> {
            switch (operacion) {
                case "obtener" -> servicio.obtener(999L);
                case "actualizar" ->
                    servicio.actualizar(999L, solicitud("Full"));
                case "cambiarEstado" ->
                    servicio.cambiarEstado(999L, false);
                default -> throw new IllegalArgumentException(operacion);
            }
        })
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Pack tarifario no encontrado: 999");

        verify(packs, never()).saveAndFlush(any(PackTarifario.class));
    }

    private PackRequest solicitud(String nombre) {
        return new PackRequest(
                nombre,
                "Beneficios del pack",
                true,
                true,
                true,
                new BigDecimal("1.50"));
    }

    private PackTarifario packExistente() {
        return PackTarifario.builder()
                .id(1L)
                .nombre("Full")
                .descripcion("Configuración anterior")
                .incluyeEquipajeCabina(true)
                .piezasBodegaIncluidas(2)
                .kgPorPiezaBodega(23)
                .incluyeSeleccionAsiento(false)
                .incluyeEmbarquePrioritario(false)
                .permiteCambios(false)
                .multiplicadorMillas(BigDecimal.ONE)
                .activo(false)
                .build();
    }
}