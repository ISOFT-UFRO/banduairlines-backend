package com.banduairlines.controller.reserva;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.banduairlines.dto.reserva.CrearReservaRequest;
import com.banduairlines.dto.reserva.ReservaCreadaResponse;
import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.dto.reserva.TotalResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.security.UsuarioAutenticado;
import com.banduairlines.service.reserva.ReservaCancelacionService;
import com.banduairlines.service.reserva.ReservaConsultaService;
import com.banduairlines.service.reserva.ReservaCreacionService;

@ExtendWith(MockitoExtension.class)
class ReservaControllerTest {

    private static final String VALIDO = """
            {"alternativaId":"ALT-1","tramos":[{"tramoId":10,"cabina":"ECONOMY"}],"pack":"Light",
             "cantidadPasajeros":{"adultos":1,"ninos":0,"bebes":0}}
            """;

    @Mock private ReservaCreacionService reservaCreacionService;
    @Mock private ReservaConsultaService reservaConsultaService;
    @Mock private ReservaCancelacionService reservaCancelacionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReservaController(reservaCreacionService, reservaConsultaService, reservaCancelacionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    static ReservaResponse reserva(EstadoReserva estado) {
        return new ReservaResponse(42L, "ABC234", estado, OffsetDateTime.of(2026, 11, 1, 18, 15, 0, 0, ZoneOffset.UTC),
                "Light", new ReservaResponse.CantidadPasajeros(1, 0, 0), List.of(), false, List.of(), List.of(),
                List.of(), null, null, new TotalResponse(List.of(), 0, 29990, "CLP"));
    }

    // ------------------------------------------ endpoint 1

    @Test
    void crearReserva_valida_responde201ConCodigoYToken() throws Exception {
        when(reservaCreacionService.crear(any(CrearReservaRequest.class))).thenReturn(new ReservaCreadaResponse(
                42L, "ABC234", EstadoReserva.PENDIENTE_PAGO, "tok", OffsetDateTime.of(2026, 11, 1, 18, 15, 0, 0, ZoneOffset.UTC),
                new TotalResponse(List.of(new TotalResponse.Linea("TARIFA", "Tarifa (1 boleto)", 29990)), 0, 29990, "CLP")));

        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservaId").value(42))
                .andExpect(jsonPath("$.codigoReserva").value("ABC234"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.compraToken").value("tok"))
                .andExpect(jsonPath("$.total.totalCLP").value(29990));
    }

    @Test
    void crearReserva_sinTramos_responde400Validacion() throws Exception {
        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tramos":[],"pack":"Light","cantidadPasajeros":{"adultos":1,"ninos":0,"bebes":0}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
        verifyNoInteractions(reservaCreacionService);
    }

    @Test
    void crearReserva_ceroPasajerosOTramoRepetido_responde400Validacion() throws Exception {
        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tramos":[{"tramoId":10,"cabina":"ECONOMY"}],"pack":"Light",
                         "cantidadPasajeros":{"adultos":0,"ninos":0,"bebes":0}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tramos":[{"tramoId":10,"cabina":"ECONOMY"},{"tramoId":10,"cabina":"ECONOMY"}],"pack":"Light",
                         "cantidadPasajeros":{"adultos":1,"ninos":0,"bebes":0}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
        verifyNoInteractions(reservaCreacionService);
    }

    @Test
    void crearReserva_cabinaInexistente_responde400PeticionMalformada() throws Exception {
        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tramos":[{"tramoId":10,"cabina":"PRIMERA"}],"pack":"Light",
                         "cantidadPasajeros":{"adultos":1,"ninos":0,"bebes":0}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PETICION_MALFORMADA"));
    }

    @Test
    void crearReserva_reglasDeNegocio_respondenConSuCodigo() throws Exception {
        when(reservaCreacionService.crear(any(CrearReservaRequest.class)))
                .thenThrow(new ReglaNegocioException("Cada bebé debe viajar con un adulto").conCodigo("BEBES_SIN_ADULTO"))
                .thenThrow(new ConflictoException("No quedan asientos").conCodigo("SIN_DISPONIBILIDAD"));

        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("BEBES_SIN_ADULTO"));
        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SIN_DISPONIBILIDAD"));
    }

    // ---------------------------------------------- endpoint 2

    @Test
    void obtenerReserva_conToken_responde200() throws Exception {
        when(reservaConsultaService.obtener(42L, "tok")).thenReturn(reserva(EstadoReserva.PENDIENTE_PAGO));

        mockMvc.perform(get("/api/v1/reservas/42").header("X-Compra-Token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoReserva").value("ABC234"))
                .andExpect(jsonPath("$.pasajerosRegistrados").value(false))
                .andExpect(jsonPath("$.cantidadPasajeros.adultos").value(1));
    }

    @Test
    void obtenerReserva_erroresDeAcceso_respondenConElEstadoDelProyecto() throws Exception {
        when(reservaConsultaService.obtener(42L, null))
                .thenThrow(new AuthenticationCredentialsNotFoundException("falta credencial"))
                .thenThrow(new AccessDeniedException("otra persona"))
                .thenThrow(new RecursoNoEncontradoException("Reserva", 42L));

        mockMvc.perform(get("/api/v1/reservas/42"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
        // GlobalExceptionHandler responde 403 solo si hay un usuario autenticado (si es anónimo, 401):
        // es el caso real de ReservaAcceso, un PASAJERO con sesion que pide la reserva de otro
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UsuarioAutenticado(8L, "otro@correo.cl", RolUsuario.PASAJERO), null, List.of()));
        try {
            mockMvc.perform(get("/api/v1/reservas/42"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
        } finally {
            SecurityContextHolder.clearContext();
        }
        mockMvc.perform(get("/api/v1/reservas/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NO_ENCONTRADO"));
    }

    // ------------------------------------------------------------ endpoint 3

    @Test
    void cancelarReserva_responde200ConLaReservaCancelada() throws Exception {
        when(reservaCancelacionService.cancelar(42L, "tok")).thenReturn(reserva(EstadoReserva.CANCELADA));

        mockMvc.perform(post("/api/v1/reservas/42/cancelacion").header("X-Compra-Token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    void cancelarReserva_yaConfirmada_responde409EstadoInvalido() throws Exception {
        when(reservaCancelacionService.cancelar(42L, "tok"))
                .thenThrow(new ConflictoException("La reserva está CONFIRMADA").conCodigo("ESTADO_INVALIDO"));

        mockMvc.perform(post("/api/v1/reservas/42/cancelacion").header("X-Compra-Token", "tok"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ESTADO_INVALIDO"));
    }
}
