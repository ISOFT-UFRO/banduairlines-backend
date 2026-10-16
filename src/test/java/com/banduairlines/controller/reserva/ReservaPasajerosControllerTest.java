package com.banduairlines.controller.reserva;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.banduairlines.dto.reserva.ContactoRequest;
import com.banduairlines.dto.reserva.ContactoResponse;
import com.banduairlines.dto.reserva.PasajeroResponse;
import com.banduairlines.dto.reserva.PasajerosRegistradosResponse;
import com.banduairlines.dto.reserva.RegistrarPasajerosRequest;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.enums.TipoDocumento;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.service.reserva.ReservaContactoService;
import com.banduairlines.service.reserva.ReservaPasajerosService;

@ExtendWith(MockitoExtension.class)
class ReservaPasajerosControllerTest {

    private static final String PASAJEROS = """
            {"pasajeros":[{"ref":"a1","tipo":"ADULTO","nombres":"Tomás","apellidos":"Rojas",
              "fechaNacimiento":"1990-05-01","tipoDocumento":"RUT","numeroDocumento":"11.111.111-1"}]}
            """;

    @Mock private ReservaPasajerosService reservaPasajerosService;
    @Mock private ReservaContactoService reservaContactoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReservaPasajerosController(reservaPasajerosService, reservaContactoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ------------------------------------------ endpoint 4

    @Test
    void registrarPasajeros_valido_responde200ConIds() throws Exception {
        when(reservaPasajerosService.registrar(eq(42L), eq("tok"), any(RegistrarPasajerosRequest.class)))
                .thenReturn(new PasajerosRegistradosResponse(List.of(new PasajeroResponse(7L, "a1", TipoPasajero.ADULTO,
                        "Tomás", "Rojas", LocalDate.of(1990, 5, 1), TipoDocumento.RUT, "11.111.111-1", null))));

        mockMvc.perform(put("/api/v1/reservas/42/pasajeros").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content(PASAJEROS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasajeros[0].pasajeroId").value(7))
                .andExpect(jsonPath("$.pasajeros[0].ref").value("a1"))
                .andExpect(jsonPath("$.pasajeros[0].fechaNacimiento").value("1990-05-01"));
    }

    @Test
    void registrarPasajeros_documentoReservadoORefRepetida_responde400() throws Exception {
        mockMvc.perform(put("/api/v1/reservas/42/pasajeros").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content(PASAJEROS.replace("11.111.111-1", "PEND-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
        mockMvc.perform(put("/api/v1/reservas/42/pasajeros").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"pasajeros":[
                                 {"ref":"a1","tipo":"ADULTO","nombres":"A","apellidos":"B","fechaNacimiento":"1990-05-01","tipoDocumento":"RUT","numeroDocumento":"1"},
                                 {"ref":"a1","tipo":"ADULTO","nombres":"C","apellidos":"D","fechaNacimiento":"1991-05-01","tipoDocumento":"RUT","numeroDocumento":"2"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
        verifyNoInteractions(reservaPasajerosService);
    }

    @Test
    void registrarPasajeros_reglaDeNegocio_responde422ConSuCodigo() throws Exception {
        when(reservaPasajerosService.registrar(eq(42L), eq("tok"), any(RegistrarPasajerosRequest.class)))
                .thenThrow(new ReglaNegocioException("x").conCodigo("CANTIDAD_PASAJEROS_DISTINTA"));

        mockMvc.perform(put("/api/v1/reservas/42/pasajeros").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content(PASAJEROS))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("CANTIDAD_PASAJEROS_DISTINTA"));
    }

    // -------------------------------------------------------- endpoint 5

    @Test
    void registrarContacto_valido_responde200() throws Exception {
        when(reservaContactoService.registrar(eq(42L), eq("tok"), any(ContactoRequest.class)))
                .thenReturn(new ContactoResponse("tomas@correo.cl", "+56911112222"));

        mockMvc.perform(put("/api/v1/reservas/42/contacto").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"tomas@correo.cl\",\"telefono\":\"+56911112222\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("tomas@correo.cl"));
    }

    @Test
    void registrarContacto_correoInvalidoOSinTelefono_responde400() throws Exception {
        mockMvc.perform(put("/api/v1/reservas/42/contacto").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"no-es-correo\",\"telefono\":\"+56911112222\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
        mockMvc.perform(put("/api/v1/reservas/42/contacto").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"a@b.cl\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
        verifyNoInteractions(reservaContactoService);
    }

    @Test
    void registrarContacto_compraVencida_responde409() throws Exception {
        when(reservaContactoService.registrar(eq(42L), eq("tok"), any(ContactoRequest.class)))
                .thenThrow(new ConflictoException("Venció").conCodigo("RESERVA_EXPIRADA"));

        mockMvc.perform(put("/api/v1/reservas/42/contacto").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"tomas@correo.cl\",\"telefono\":\"+56911112222\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVA_EXPIRADA"));
    }
}
