package com.banduairlines.controller.admin;

import com.banduairlines.dto.admin.EquipajePackRespuesta;
import com.banduairlines.dto.admin.EquipajePackSolicitud;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.service.admin.EquipajePackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EquipajePackControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EquipajePackService servicio;

    @InjectMocks
    private EquipajePackController controlador;

    private final EquipajePackRespuesta respuesta =
        new EquipajePackRespuesta(
            1L,
            "Prueba RF06",
            true,
            1,
            23
        );

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(controlador)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void obtenerDevuelveEquipajeDelPack() throws Exception {
        when(servicio.obtener(1L)).thenReturn(respuesta);

        mockMvc.perform(get("/api/v1/admin/packs/1/equipaje"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.packId").value(1))
            .andExpect(jsonPath("$.nombrePack").value("Prueba RF06"))
            .andExpect(jsonPath("$.incluyeEquipajeCabina").value(true))
            .andExpect(jsonPath("$.piezasBodegaIncluidas").value(1))
            .andExpect(jsonPath("$.kgPorPiezaBodega").value(23));

        verify(servicio).obtener(1L);
    }

    @Test
    void actualizarRecibeElJsonYDevuelveEquipaje() throws Exception {
        EquipajePackSolicitud solicitud = new EquipajePackSolicitud(
            true,
            1,
            23
        );

        when(servicio.actualizar(1L, solicitud))
            .thenReturn(respuesta);

        mockMvc.perform(put("/api/v1/admin/packs/1/equipaje")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "incluyeEquipajeCabina": true,
                        "piezasBodegaIncluidas": 1,
                        "kgPorPiezaBodega": 23
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.packId").value(1))
            .andExpect(jsonPath("$.incluyeEquipajeCabina").value(true))
            .andExpect(jsonPath("$.piezasBodegaIncluidas").value(1))
            .andExpect(jsonPath("$.kgPorPiezaBodega").value(23));

        verify(servicio).actualizar(1L, solicitud);
    }

    @Test
    void actualizarRechazaCamposObligatoriosAusentes()
            throws Exception {

        mockMvc.perform(put("/api/v1/admin/packs/1/equipaje")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDACION"))
            .andExpect(jsonPath("$.details.length()").value(3));

        verifyNoInteractions(servicio);
    }
}