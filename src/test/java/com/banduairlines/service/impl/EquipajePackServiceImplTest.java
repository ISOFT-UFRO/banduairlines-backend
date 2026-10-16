package com.banduairlines.service.impl;

import com.banduairlines.dto.admin.EquipajePackSolicitud;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.repository.PackTarifarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipajePackServiceImplTest {

    @Mock
    private PackTarifarioRepository packs;

    @InjectMocks
    private EquipajePackServiceImpl servicio;

    @Test
    void actualizarModificaEquipajeYConservaLosDemasBeneficios() {
        PackTarifario pack = PackTarifario.builder()
                .id(1L)
                .nombre("Prueba RF06")
                .descripcion("Beneficios del pack de prueba")
                .incluyeEquipajeCabina(false)
                .piezasBodegaIncluidas(0)
                .kgPorPiezaBodega(0)
                .incluyeSeleccionAsiento(true)
                .incluyeEmbarquePrioritario(true)
                .permiteCambios(true)
                .multiplicadorMillas(new BigDecimal("1.25"))
                .activo(true)
                .build();

        when(packs.findById(1L))
                .thenReturn(Optional.of(pack));

        when(packs.saveAndFlush(pack))
                .thenReturn(pack);

        EquipajePackSolicitud solicitud = new EquipajePackSolicitud(
                true,
                2,
                23);

        var respuesta = servicio.actualizar(1L, solicitud);

        assertThat(respuesta.packId()).isEqualTo(1L);
        assertThat(respuesta.nombrePack()).isEqualTo("Prueba RF06");
        assertThat(respuesta.incluyeEquipajeCabina()).isTrue();
        assertThat(respuesta.piezasBodegaIncluidas()).isEqualTo(2);
        assertThat(respuesta.kgPorPiezaBodega()).isEqualTo(23);

        assertThat(pack.getNombre()).isEqualTo("Prueba RF06");
        assertThat(pack.getDescripcion())
                .isEqualTo("Beneficios del pack de prueba");
        assertThat(pack.getIncluyeSeleccionAsiento()).isTrue();
        assertThat(pack.getIncluyeEmbarquePrioritario()).isTrue();
        assertThat(pack.getPermiteCambios()).isTrue();
        assertThat(pack.getMultiplicadorMillas())
                .isEqualByComparingTo("1.25");
        assertThat(pack.getActivo()).isTrue();

        verify(packs).saveAndFlush(pack);
    }
}