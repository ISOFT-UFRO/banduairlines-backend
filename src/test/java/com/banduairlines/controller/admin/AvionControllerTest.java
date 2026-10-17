package com.banduairlines.controller.admin;

import com.banduairlines.dto.admin.AvionRequestDto;
import com.banduairlines.dto.admin.AvionResponseDto;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.model.enums.Estado;
import com.banduairlines.service.admin.AvionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AvionControllerTest {

    @Mock
    private AvionService servicio;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validador;

    @BeforeEach
    void preparar() {
        validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AvionController(servicio))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validador)
                .build();
    }

    @AfterEach
    void cerrarValidador() {
        validador.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rechazarFilaInvalidaSinLlamarAlServicio(int fila)
            throws Exception {

        mockMvc.perform(post("/api/v1/admin/aeronaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoConFila(fila)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));

        verifyNoInteractions(servicio);
    }

    @Test
    void rechazarAsientoNuloSinLlamarAlServicio() throws Exception {
        String cuerpo = """
                {
                    "matricula": "CC-BNA",
                    "modelo": "Airbus A320",
                    "asientos": [null]
                }
                """;

        mockMvc.perform(post("/api/v1/admin/aeronaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));

        verifyNoInteractions(servicio);
    }

    @Test
    void aceptarSolicitudValidaYDevolverCreado() throws Exception {
        AvionResponseDto respuesta = AvionResponseDto.builder()
                .id(1L)
                .matricula("CC-BNA")
                .modelo("Airbus A320")
                .estado(Estado.ACTIVA)
                .totalAsientos(1)
                .build();

        when(servicio.crearAvion(any(AvionRequestDto.class)))
                .thenReturn(respuesta);

        mockMvc.perform(post("/api/v1/admin/aeronaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoConFila(1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.totalAsientos").value(1));

        verify(servicio).crearAvion(any(AvionRequestDto.class));
    }

    private String cuerpoConFila(int fila) {
        return """
                {
                    "matricula": "CC-BNA",
                    "modelo": "Airbus A320",
                    "asientos": [
                        {
                            "fila": %d,
                            "letra": "A",
                            "claseCabina": "ECONOMY",
                            "esPreferente": false
                        }
                    ]
                }
                """.formatted(fila);
    }
}