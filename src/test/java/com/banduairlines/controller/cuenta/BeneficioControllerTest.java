package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.RespuestaBeneficio;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.model.enums.MotivoBloqueo;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.service.cuenta.CatalogoBeneficioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BeneficioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CatalogoBeneficioService catalogoBeneficioService;

    @InjectMocks
    private BeneficioController beneficioController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(beneficioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listar_devuelveElCatalogoConMotivoDeBloqueo() throws Exception {
        when(catalogoBeneficioService.listarActual()).thenReturn(List.of(
                new RespuestaBeneficio(1L, "Equipaje extra", "Una maleta adicional", Nivel.BASICO, 100, true, null),
                new RespuestaBeneficio(2L, "Sala VIP", "Acceso a sala VIP", Nivel.ORO, 800, false,
                        MotivoBloqueo.NIVEL_INSUFICIENTE)));

        mockMvc.perform(get("/api/v1/fidelizacion/beneficios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disponible").value(true))
                .andExpect(jsonPath("$[1].disponible").value(false))
                .andExpect(jsonPath("$[1].motivoBloqueo").value("NIVEL_INSUFICIENTE"));
    }
}
