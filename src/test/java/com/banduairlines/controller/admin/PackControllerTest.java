package com.banduairlines.controller.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import com.banduairlines.dto.admin.PackRequest;
import com.banduairlines.dto.admin.PackResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.service.admin.PackService;

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
class PackControllerTest {

    private static final String RUTA = "/api/v1/admin/packs";

    private static final String JSON_VALIDO = """
            {
                "nombre": "Full",
                "descripcion": "Beneficios",
                "incluyeSeleccionAsiento": true,
                "incluyeEmbarquePrioritario": true,
                "permiteCambios": true,
                "multiplicadorMillas": 1.50
            }
            """;

    @Mock
    private PackService servicio;

    @InjectMocks
    private PackController controlador;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void crearDevuelve201YLosDatosDelPack() throws Exception {
        when(servicio.crear(any(PackRequest.class)))
                .thenReturn(respuesta(true));

        mockMvc.perform(post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Full"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.incluyeSeleccionAsiento").value(true));

        verify(servicio).crear(solicitud());
    }

    @ParameterizedTest
    @MethodSource("solicitudesInvalidas")
    void crearRechazaDatosInvalidosSinLlamarAlServicio(
            String cuerpo) throws Exception {

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
                JSON_VALIDO.replace("\"Full\"", "\"   \""),
                JSON_VALIDO.replace("\"Full\"",
                        "\"" + "a".repeat(31) + "\""),
                JSON_VALIDO.replace(
                        "\"incluyeSeleccionAsiento\": true",
                        "\"incluyeSeleccionAsiento\": null"),
                JSON_VALIDO.replace(
                        "\"incluyeEmbarquePrioritario\": true",
                        "\"incluyeEmbarquePrioritario\": null"),
                JSON_VALIDO.replace(
                        "\"permiteCambios\": true",
                        "\"permiteCambios\": null"),
                JSON_VALIDO.replace("1.50", "-1.00"),
                JSON_VALIDO.replace("1.50", "10.00"),
                JSON_VALIDO.replace("1.50", "1.001"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void listarRecibeElFiltroDeEstado(boolean activo) throws Exception {
        when(servicio.listar(activo))
                .thenReturn(List.of(respuesta(activo)));

        mockMvc.perform(get(RUTA)
                .param("activo", Boolean.toString(activo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].activo").value(activo));

        verify(servicio).listar(activo);
    }

    @Test
    void actualizarRecibeLaSolicitudYDevuelve200() throws Exception {
        when(servicio.actualizar(eq(1L), any(PackRequest.class)))
                .thenReturn(respuesta(true));

        mockMvc.perform(put(RUTA + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Full"));

        verify(servicio).actualizar(1L, solicitud());
    }

    @Test
    void cambiarEstadoRecibeFalseYDevuelvePackInactivo() throws Exception {
        when(servicio.cambiarEstado(1L, false))
                .thenReturn(respuesta(false));

        mockMvc.perform(patch(RUTA + "/1/activo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"activo": false}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        verify(servicio).cambiarEstado(1L, false);
    }

    @Test
    void cambiarEstadoRechazaCampoAusente() throws Exception {
        mockMvc.perform(patch(RUTA + "/1/activo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));

        verifyNoInteractions(servicio);
    }

    @Test
    void obtenerDevuelve404SiElPackNoExiste() throws Exception {
        when(servicio.obtener(999L))
                .thenThrow(new RecursoNoEncontradoException(
                        "Pack tarifario", 999L));

        mockMvc.perform(get(RUTA + "/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("RECURSO_NO_ENCONTRADO"));
    }

    @Test
    void crearDevuelve409PorNombreDuplicado() throws Exception {
        when(servicio.crear(any(PackRequest.class)))
                .thenThrow(new ConflictoException(
                        "Ya existe un pack tarifario con ese nombre")
                        .conCodigo("PACK_DUPLICADO"));

        mockMvc.perform(post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PACK_DUPLICADO"));
    }

    private PackRequest solicitud() {
        return new PackRequest(
                "Full", "Beneficios",
                true, true, true,
                new BigDecimal("1.50"));
    }

    private PackResponse respuesta(boolean activo) {
        return new PackResponse(
                1L, "Full", "Beneficios",
                false, 0, 0,
                true, true, true,
                new BigDecimal("1.50"),
                activo);
    }
}