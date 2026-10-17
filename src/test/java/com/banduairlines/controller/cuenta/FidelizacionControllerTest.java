package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.ProgresoNivel;
import com.banduairlines.dto.cuenta.RespuestaFidelizacion;
import com.banduairlines.dto.cuenta.RespuestaMovimientoPuntos;
import com.banduairlines.dto.cuenta.RespuestaPaginada;
import com.banduairlines.exception.CodigosError;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.ServicioExternoException;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.persistence.cuenta.TipoRegistroPuntos;
import com.banduairlines.service.cuenta.CodigosFidelizacion;
import com.banduairlines.service.cuenta.FidelizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FidelizacionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FidelizacionService fidelizacionService;

    @InjectMocks
    private FidelizacionController fidelizacionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fidelizacionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void consultar_responde200ConElResumen() throws Exception {
        when(fidelizacionService.consultarActual()).thenReturn(new RespuestaFidelizacion(
                900, new BigDecimal("12500.5"), Nivel.PLATA, new BigDecimal("1.2"),
                new ProgresoNivel(Nivel.ORO, 25000L, 12500, 16), List.of(), List.of()));

        mockMvc.perform(get("/api/v1/cuentas/actual/fidelizacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puntosDisponibles").value(900))
                .andExpect(jsonPath("$.nivelActual").value("PLATA"))
                .andExpect(jsonPath("$.progreso.nivelSiguiente").value("ORO"))
                .andExpect(jsonPath("$.progreso.porcentaje").value(16));
    }

    @Test
    void consultar_conMillasNoDisponibles_responde502() throws Exception {
        when(fidelizacionService.consultarActual()).thenThrow(
                new ServicioExternoException("Sin historial").conCodigo(CodigosFidelizacion.MILLAS_NO_DISPONIBLES));

        mockMvc.perform(get("/api/v1/cuentas/actual/fidelizacion"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(CodigosFidelizacion.MILLAS_NO_DISPONIBLES));
    }

    @Test
    void movimientos_pasaLaPaginaYElTamano() throws Exception {
        RespuestaMovimientoPuntos movimiento = new RespuestaMovimientoPuntos(
                51L, TipoRegistroPuntos.CANJE, -200, null, "Canje de Sala VIP", null, 3L);
        when(fidelizacionService.listarMovimientosActual(1, 5))
                .thenReturn(new RespuestaPaginada<>(List.of(movimiento), 1, 5, 6, 2));

        mockMvc.perform(get("/api/v1/cuentas/actual/fidelizacion/movimientos")
                        .param("pagina", "1")
                        .param("tamano", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idMovimiento").value(51))
                .andExpect(jsonPath("$.contenido[0].tipo").value("CANJE"))
                .andExpect(jsonPath("$.contenido[0].cantidadPuntos").value(-200))
                .andExpect(jsonPath("$.totalElementos").value(6))
                .andExpect(jsonPath("$.totalPaginas").value(2));

        verify(fidelizacionService).listarMovimientosActual(1, 5);
    }

    @Test
    void movimientos_sinParametrosUsaPaginaCeroYTamano20() throws Exception {
        when(fidelizacionService.listarMovimientosActual(0, 20))
                .thenReturn(new RespuestaPaginada<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/cuentas/actual/fidelizacion/movimientos"))
                .andExpect(status().isOk());

        verify(fidelizacionService).listarMovimientosActual(0, 20);
    }

    @Test
    void movimientos_conTamanoInvalido_responde400() throws Exception {
        when(fidelizacionService.listarMovimientosActual(0, 500))
                .thenThrow(new ValidacionException("El tamaño de página debe estar entre 1 y 100"));

        mockMvc.perform(get("/api/v1/cuentas/actual/fidelizacion/movimientos").param("tamano", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CodigosError.VALIDACION));
    }
}
