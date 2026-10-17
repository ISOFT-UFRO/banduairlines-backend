package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.RespuestaCanje;
import com.banduairlines.dto.cuenta.RespuestaMovimientoPuntos;
import com.banduairlines.exception.CodigosError;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.model.enums.TipoMovimiento;
import com.banduairlines.service.cuenta.CanjeService;
import com.banduairlines.service.cuenta.CanjeService.ResultadoCanje;
import com.banduairlines.service.cuenta.CodigosFidelizacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CanjeControllerTest {

    private static final String RUTA = "/api/v1/fidelizacion/canjes";
    private static final String CLAVE = "clave-prueba-1";

    private MockMvc mockMvc;

    @Mock
    private CanjeService canjeService;

    @InjectMocks
    private CanjeController canjeController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(canjeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void canjeNuevo_responde201() throws Exception {
        when(canjeService.canjearActual(3L, CLAVE)).thenReturn(
                new ResultadoCanje(respuestaCanje(), true));

        mockMvc.perform(post(RUTA)
                        .header(CanjeController.ENCABEZADO_CLAVE, CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idBeneficio\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCanje").value(50))
                .andExpect(jsonPath("$.saldoAnterior").value(1000))
                .andExpect(jsonPath("$.puntosDescontados").value(200))
                .andExpect(jsonPath("$.saldoActual").value(800))
                .andExpect(jsonPath("$.movimiento.tipo").value("CANJE"))
                .andExpect(jsonPath("$.movimiento.cantidadPuntos").value(-200));
    }

    @Test
    void claveRepetida_responde200ConElCanjeOriginal() throws Exception {
        when(canjeService.canjearActual(3L, CLAVE)).thenReturn(
                new ResultadoCanje(respuestaCanje(), false));

        mockMvc.perform(post(RUTA)
                        .header(CanjeController.ENCABEZADO_CLAVE, CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idBeneficio\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCanje").value(50))
                .andExpect(jsonPath("$.saldoActual").value(800));
    }

    @Test
    void puntosInsuficientes_responde422() throws Exception {
        when(canjeService.canjearActual(3L, CLAVE)).thenThrow(
                new ReglaNegocioException("No tienes puntos suficientes")
                        .conCodigo(CodigosFidelizacion.PUNTOS_INSUFICIENTES));

        mockMvc.perform(post(RUTA)
                        .header(CanjeController.ENCABEZADO_CLAVE, CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idBeneficio\":3}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value(CodigosFidelizacion.PUNTOS_INSUFICIENTES));
    }

    @Test
    void claveUsadaConOtroBeneficio_responde409() throws Exception {
        when(canjeService.canjearActual(3L, CLAVE)).thenThrow(
                new ConflictoException("La clave ya se usó").conCodigo(CodigosFidelizacion.CANJE_DUPLICADO));

        mockMvc.perform(post(RUTA)
                        .header(CanjeController.ENCABEZADO_CLAVE, CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idBeneficio\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(CodigosFidelizacion.CANJE_DUPLICADO));
    }

    @Test
    void sinIdBeneficio_responde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post(RUTA)
                        .header(CanjeController.ENCABEZADO_CLAVE, CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CodigosError.VALIDACION));

        verifyNoInteractions(canjeService);
    }

    @Test
    void sinClaveIdempotencia_responde400() throws Exception {
        when(canjeService.canjearActual(3L, null)).thenThrow(
                new ValidacionException("El encabezado Clave-Idempotencia es obligatorio"));

        mockMvc.perform(post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idBeneficio\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CodigosError.VALIDACION));
    }

    private static RespuestaCanje respuestaCanje() {
        return new RespuestaCanje(50L, 1000, 200, 800, new RespuestaMovimientoPuntos(
                50L, TipoMovimiento.CANJE, -200, null, "Canje de Sala VIP", null, 3L));
    }
}
