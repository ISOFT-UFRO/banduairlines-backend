package com.banduairlines.controller.admin;

import com.banduairlines.dto.admin.AsientoResponse;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.service.admin.AsientoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AsientoControllerTest {

        @Mock
        private AsientoService asientoService;

        @InjectMocks
        private AsientoController asientoController;

        private MockMvc mockMvc;

        private final AsientoResponse respuestaEsperada = new AsientoResponse(
                        10L, 1L, 1, "A", ClaseCabina.BUSINESS, true);

        @BeforeEach
        void preparar() {
                mockMvc = MockMvcBuilders.standaloneSetup(asientoController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        @Test
        void listarPorAeronave_sinClase() throws Exception {
                when(asientoService.obtenerAsientosPorAeronave(1L))
                                .thenReturn(List.of(respuestaEsperada));

                mockMvc.perform(get("/api/v1/admin/asientos/aeronave/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].id").value(10))
                                .andExpect(jsonPath("$[0].aeronaveId").value(1))
                                .andExpect(jsonPath("$[0].letra").value("A"))
                                .andExpect(jsonPath("$[0].aeronave").doesNotExist());

                verify(asientoService).obtenerAsientosPorAeronave(1L);
                verifyNoMoreInteractions(asientoService);
        }

        @Test
        void listarPorAeronave_conClase() throws Exception {
                when(asientoService.obtenerAsientosPorClase(
                                1L, ClaseCabina.BUSINESS))
                                .thenReturn(List.of(respuestaEsperada));

                mockMvc.perform(get("/api/v1/admin/asientos/aeronave/1")
                                .param("claseCabina", "BUSINESS"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].claseCabina").value("BUSINESS"));

                verify(asientoService).obtenerAsientosPorClase(
                                1L, ClaseCabina.BUSINESS);
                verifyNoMoreInteractions(asientoService);
        }

        @Test
        void listarPorAeronave_listaVacia() throws Exception {
                when(asientoService.obtenerAsientosPorAeronave(99L))
                                .thenReturn(List.of());

                mockMvc.perform(get("/api/v1/admin/asientos/aeronave/99"))
                                .andExpect(status().isOk())
                                .andExpect(content().json("[]"));

                verify(asientoService).obtenerAsientosPorAeronave(99L);
        }

        @Test
        void crearAsiento_retorna201() throws Exception {
                when(asientoService.crearAsiento(
                                1L, 1, "A", ClaseCabina.BUSINESS, true))
                                .thenReturn(respuestaEsperada);

                mockMvc.perform(post("/api/v1/admin/asientos")
                                .param("aeronaveId", "1")
                                .param("fila", "1")
                                .param("letra", "A")
                                .param("claseCabina", "BUSINESS")
                                .param("esPreferente", "true"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(10))
                                .andExpect(jsonPath("$.aeronaveId").value(1))
                                .andExpect(jsonPath("$.letra").value("A"))
                                .andExpect(jsonPath("$.esPreferente").value(true))
                                .andExpect(jsonPath("$.aeronave").doesNotExist());

                verify(asientoService).crearAsiento(
                                1L, 1, "A", ClaseCabina.BUSINESS, true);
        }

        @Test
        void crearAsiento_aeronaveInexistenteRetorna404() throws Exception {
                when(asientoService.crearAsiento(
                                99L, 1, "A", ClaseCabina.BUSINESS, false))
                                .thenThrow(new RecursoNoEncontradoException(
                                                "Aeronave no encontrada: 99"));

                mockMvc.perform(post("/api/v1/admin/asientos")
                                .param("aeronaveId", "99")
                                .param("fila", "1")
                                .param("letra", "A")
                                .param("claseCabina", "BUSINESS")
                                .param("esPreferente", "false"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code")
                                                .value("RECURSO_NO_ENCONTRADO"))
                                .andExpect(jsonPath("$.message")
                                                .value("Aeronave no encontrada: 99"));

                verify(asientoService).crearAsiento(
                                99L, 1, "A", ClaseCabina.BUSINESS, false);
        }
}