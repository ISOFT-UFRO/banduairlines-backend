package com.banduairlines.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.banduairlines.dto.admin.PromocionResponse;
import com.banduairlines.dto.admin.PromocionRequest;
import com.banduairlines.exception.CodigosError;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.model.enums.TipoDescuento;
import com.banduairlines.service.admin.PromocionService;

@ExtendWith(MockitoExtension.class)
class PromocionControllerTest {

    private static final String RUTA = "/api/v1/admin/promociones";
    private static final Long ID = 1L;
    private static final String CODIGO = "DESC10";
    private static final BigDecimal VALOR = new BigDecimal("10.00");
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 12, 1, 0, 0);
    private static final LocalDateTime EXPIRACION = LocalDateTime.of(2026, 12, 31, 23, 59, 59);

    private static final String SOLICITUD_VALIDA = """
            {"codigo": "DESC10", "tipoDescuento": "PORCENTAJE", "valor": 10.00,
             "vigenteDesde": "2026-12-01T00:00:00", "vigenteHasta": "2026-12-31T23:59:59", "usosMaximos": 100}
            """;
    private static final String SOLICITUD_SIN_CODIGO = SOLICITUD_VALIDA.replace("\"DESC10\"", "\"\"");

    private MockMvc mockMvc;

    @Mock
    private PromocionService promocionService;

    @InjectMocks
    private PromocionController promocionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(promocionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listar_cuandoHayPromociones_retorna200ConLasPromociones() throws Exception {
        when(promocionService.listar(null)).thenReturn(List.of(respuesta()));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value(CODIGO));
    }

    @Test
    void crear_cuandoLaSolicitudEsValida_retorna201ConLaPromocionCreada() throws Exception {
        when(promocionService.crear(any(PromocionRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(SOLICITUD_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(CODIGO));
    }

    @Test
    void crear_cuandoLaSolicitudEsInvalida_retorna400ConElCodigoDeValidacion() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(SOLICITUD_SIN_CODIGO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CodigosError.VALIDACION));
    }

    @Test
    void cambiarEstado_cuandoSeDesactiva_retorna200ConLaPromocionInactiva() throws Exception {
        when(promocionService.cambiarEstado(ID, false)).thenReturn(respuestaInactiva());

        mockMvc.perform(patch(RUTA + "/" + ID + "/activo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));
    }

    private PromocionResponse respuesta() {
        return new PromocionResponse(ID, CODIGO, TipoDescuento.PORCENTAJE, VALOR, INICIO, EXPIRACION, 100, 0, true);
    }

    private PromocionResponse respuestaInactiva() {
        return new PromocionResponse(ID, CODIGO, TipoDescuento.PORCENTAJE, VALOR, INICIO, EXPIRACION, 100, 0, false);
    }
}
