package com.banduairlines.service;

import com.banduairlines.dto.cuenta.ActualizacionFidelizacion;
import com.banduairlines.dto.cuenta.RespuestaBeneficio;
import com.banduairlines.dto.cuenta.MotivoBloqueo;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.service.cuenta.ActualizacionFidelizacionService;
import com.banduairlines.service.cuenta.BeneficioConfigurado;
import com.banduairlines.service.cuenta.CatalogoBeneficios;
import com.banduairlines.service.cuenta.EvaluadorBeneficio;
import com.banduairlines.service.cuenta.PropiedadesFidelizacion;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos;
import com.banduairlines.service.impl.AccesoMovimientosPuntos;
import com.banduairlines.service.impl.CatalogoBeneficioServiceImpl;
import com.banduairlines.service.impl.MapeadorFidelizacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoBeneficioServiceImplTest {

    @Mock
    private ActualizacionFidelizacionService actualizacion;
    @Mock
    private FuenteMovimientosPuntos fuente;
    @Mock
    private AccesoMovimientosPuntos acceso;

    private CatalogoBeneficioServiceImpl servicio(BeneficioConfigurado... beneficios) {
        return new CatalogoBeneficioServiceImpl(actualizacion,
                new CatalogoBeneficios(new PropiedadesFidelizacion(List.of(beneficios))),
                acceso, new MapeadorFidelizacion(new EvaluadorBeneficio()));
    }

    @Test
    void catalogo_indicaDisponibilidadYMotivoDeBloqueo() {
        when(actualizacion.actualizarActual()).thenReturn(
                new ActualizacionFidelizacion(BigDecimal.ZERO, 300, Nivel.BASICO, BigDecimal.ONE));
        when(acceso.exigir()).thenReturn(fuente);
        when(fuente.contarCanjes(Set.of(1L, 2L, 3L, 5L))).thenReturn(Map.of(5L, 10L));

        List<RespuestaBeneficio> catalogo = servicio(
                new BeneficioConfigurado(1L, "Equipaje extra", null, Nivel.BASICO, 100, 10, true),
                new BeneficioConfigurado(2L, "Upgrade", null, Nivel.PLATA, 110, 10, true),
                new BeneficioConfigurado(3L, "Asiento", null, Nivel.BASICO, 500, 10, true),
                new BeneficioConfigurado(4L, "Kit de viaje", null, Nivel.BASICO, 50, 10, false),
                new BeneficioConfigurado(5L, "Snack", null, Nivel.BASICO, 60, 10, true)).listarActual();

        assertEquals(5, catalogo.size());
        RespuestaBeneficio kit = buscar(catalogo, 4L);
        RespuestaBeneficio snack = buscar(catalogo, 5L);
        RespuestaBeneficio equipaje = buscar(catalogo, 1L);
        assertEquals(MotivoBloqueo.BENEFICIO_AGOTADO, kit.motivoBloqueo());
        assertEquals(MotivoBloqueo.BENEFICIO_AGOTADO, snack.motivoBloqueo());
        assertTrue(equipaje.disponible());
        assertNull(equipaje.motivoBloqueo());
        assertEquals(MotivoBloqueo.NIVEL_INSUFICIENTE, buscar(catalogo, 2L).motivoBloqueo());
        assertEquals(MotivoBloqueo.PUNTOS_INSUFICIENTES, buscar(catalogo, 3L).motivoBloqueo());
        assertFalse(buscar(catalogo, 3L).disponible());
    }

    @Test
    void catalogo_rechazaRolesQueNoParticipan() {
        when(actualizacion.actualizarActual()).thenThrow(new AccessDeniedException("Solo pasajeros"));

        assertThrows(AccessDeniedException.class, () -> servicio(
                new BeneficioConfigurado(1L, "Equipaje extra", null, Nivel.BASICO, 100, 10, true)).listarActual());
    }

    private static RespuestaBeneficio buscar(List<RespuestaBeneficio> catalogo, Long id) {
        return catalogo.stream().filter(b -> b.idBeneficio().equals(id)).findFirst().orElseThrow();
    }
}
