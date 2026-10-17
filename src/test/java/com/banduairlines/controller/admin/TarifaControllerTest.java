package com.banduairlines.controller.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import com.banduairlines.dto.admin.TarifaRequest;
import com.banduairlines.dto.admin.TarifaResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.service.admin.TarifaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TarifaControllerTest {

    private static final String RUTA = "/api/v1/admin/vuelos/1/tarifas";

    private static final String JSON_VALIDO = """
            {
                "claseCabina": "ECONOMY",
                "packId": 2,
                "precio": 45990
            }
            """;

    @Mock
    private TarifaService servicio;

    @InjectMocks
    private TarifaController controlador;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "45990", "99999999" })
    void crearAceptaPreciosValidosYDevuelve201(String valor)
            throws Exception {

        BigDecimal precio = new BigDecimal(valor);

        when(servicio.crear(eq(1L), any(TarifaRequest.class)))
                .thenReturn(respuesta(precio));

        mockMvc.perform(post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO.replace("45990", valor)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.vueloId").value(1))
                .andExpect(jsonPath("$.claseCabina").value("ECONOMY"))
                .andExpect(jsonPath("$.packId").value(2))
                .andExpect(jsonPath("$.precio").value(precio.intValueExact()))
                .andExpect(jsonPath("$.moneda").value("CLP"));

        verify(servicio).crear(
                1L, new TarifaRequest(ClaseCabina.ECONOMY, 2L, precio));
    }

    @ParameterizedTest
    @MethodSource("solicitudesInvalidas")
    void crearRechazaDatosInvalidosSinLlamarAlServicio(String cuerpo)
            throws Exception {

        mockMvc.perform(post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));

        verifyNoInteractions(servicio);
    }

    static Stream<String> solicitudesInvalidas() {
        return Stream.of(
                "{}",
                JSON_VALIDO.replace("\"ECONOMY\"", "null"),
                JSON_VALIDO.replace("\"packId\": 2", "\"packId\": null"),
                JSON_VALIDO.replace("\"packId\": 2", "\"packId\": 0"),
                JSON_VALIDO.replace("45990", "null"),
                JSON_VALIDO.replace("45990", "-1"),
                JSON_VALIDO.replace("45990", "0.50"),
                JSON_VALIDO.replace("45990", "100000000"));
    }

    @Test
    void crearRechazaClaseCabinaDesconocida() throws Exception {
        mockMvc.perform(post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO.replace("ECONOMY", "FIRST")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PETICION_MALFORMADA"));

        verifyNoInteractions(servicio);
    }

    @Test
    void listarRecibeFiltroDeCabina() throws Exception {
        when(servicio.listar(1L, ClaseCabina.ECONOMY))
                .thenReturn(List.of(respuesta(new BigDecimal("45990"))));

        mockMvc.perform(get(RUTA)
                .param("claseCabina", "ECONOMY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claseCabina").value("ECONOMY"))
                .andExpect(jsonPath("$[0].nombrePack").value("Full"));

        verify(servicio).listar(1L, ClaseCabina.ECONOMY);
    }

    @Test
    void listarSinFiltroConsultaTodasLasCabinas() throws Exception {
        when(servicio.listar(1L, null)).thenReturn(List.of());

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(servicio).listar(1L, null);
    }

    @Test
    void actualizarPrecioRecibeJsonYDevuelve200() throws Exception {
        BigDecimal precio = new BigDecimal("49990");

        when(servicio.actualizarPrecio(1L, 10L, precio))
                .thenReturn(respuesta(precio));

        mockMvc.perform(put(RUTA + "/10/precio")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"precio": 49990}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.vueloId").value(1))
                .andExpect(jsonPath("$.packId").value(2))
                .andExpect(jsonPath("$.precio").value(49990));

        verify(servicio).actualizarPrecio(1L, 10L, precio);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"precio\": null}",
            "{\"precio\": -1}",
            "{\"precio\": 0.50}",
            "{\"precio\": 100000000}"
    })
    void actualizarRechazaPrecioInvalidoSinLlamarAlServicio(String cuerpo)
            throws Exception {

        mockMvc.perform(put(RUTA + "/10/precio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));

        verifyNoInteractions(servicio);
    }

    @Test
    void obtenerDevuelve404SiLaTarifaNoPerteneceAlVuelo()
            throws Exception {

        when(servicio.obtener(1L, 99L))
                .thenThrow(new RecursoNoEncontradoException("Tarifa", 99L));

        mockMvc.perform(get(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("RECURSO_NO_ENCONTRADO"));

        verify(servicio).obtener(1L, 99L);
    }

    @Test
    void crearDevuelve409PorCombinacionDuplicada() throws Exception {
        when(servicio.crear(eq(1L), any(TarifaRequest.class)))
                .thenThrow(new ConflictoException("Tarifa duplicada")
                        .conCodigo("TARIFA_DUPLICADA"));

        mockMvc.perform(post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TARIFA_DUPLICADA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "PACK_INACTIVO",
            "CABINA_NO_DISPONIBLE_EN_AERONAVE"
    })
    void crearDevuelve422PorReglaDeNegocio(String codigo)
            throws Exception {

        when(servicio.crear(eq(1L), any(TarifaRequest.class)))
                .thenThrow(new ReglaNegocioException(
                        "La configuración no permite crear la tarifa")
                        .conCodigo(codigo));

        mockMvc.perform(post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value(codigo));
    }

    private TarifaResponse respuesta(BigDecimal precio) {
        return new TarifaResponse(
                10L,
                1L,
                ClaseCabina.ECONOMY,
                2L,
                "Full",
                true,
                precio,
                "CLP");
    }
}