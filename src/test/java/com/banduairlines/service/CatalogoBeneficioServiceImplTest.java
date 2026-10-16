package com.banduairlines.service;

import com.banduairlines.dto.cuenta.ActualizacionFidelizacion;
import com.banduairlines.dto.cuenta.RespuestaBeneficio;
import com.banduairlines.model.Beneficio;
import com.banduairlines.model.enums.MotivoBloqueo;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.repository.BeneficioRepository;
import com.banduairlines.service.cuenta.ActualizacionFidelizacionService;
import com.banduairlines.service.cuenta.EvaluadorBeneficio;
import com.banduairlines.service.impl.CatalogoBeneficioServiceImpl;
import com.banduairlines.service.impl.MapeadorFidelizacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;

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
    private BeneficioRepository beneficios;

    private CatalogoBeneficioServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new CatalogoBeneficioServiceImpl(actualizacion, beneficios,
                new MapeadorFidelizacion(beneficios, new EvaluadorBeneficio()));
    }

    @Test
    void catalogo_indicaDisponibilidadYMotivoDeBloqueo() {
        when(actualizacion.actualizarActual()).thenReturn(
                new ActualizacionFidelizacion(BigDecimal.ZERO, 300, Nivel.BASICO, BigDecimal.ONE));
        when(beneficios.findAllByOrderByCostoPuntosAscNombreAsc()).thenReturn(List.of(
                beneficio(1L, Nivel.BASICO, 100, true),
                beneficio(2L, Nivel.PLATA, 100, true),
                beneficio(3L, Nivel.BASICO, 500, true),
                beneficio(4L, Nivel.BASICO, 50, false)));

        List<RespuestaBeneficio> catalogo = servicio.listarActual();

        assertEquals(4, catalogo.size());
        assertTrue(catalogo.get(0).disponible());
        assertNull(catalogo.get(0).motivoBloqueo());
        assertFalse(catalogo.get(1).disponible());
        assertEquals(MotivoBloqueo.NIVEL_INSUFICIENTE, catalogo.get(1).motivoBloqueo());
        assertEquals(MotivoBloqueo.PUNTOS_INSUFICIENTES, catalogo.get(2).motivoBloqueo());
        assertEquals(MotivoBloqueo.BENEFICIO_AGOTADO, catalogo.get(3).motivoBloqueo());
    }

    @Test
    void catalogo_rechazaRolesQueNoParticipan() {
        when(actualizacion.actualizarActual()).thenThrow(new AccessDeniedException("Solo pasajeros"));

        assertThrows(AccessDeniedException.class, () -> servicio.listarActual());
    }

    private static Beneficio beneficio(Long id, Nivel nivel, int costo, boolean disponible) {
        return Beneficio.builder()
                .idBeneficio(id)
                .nombre("Beneficio " + id)
                .nivelRequerido(nivel)
                .costoPuntos(costo)
                .disponible(disponible)
                .build();
    }
}
