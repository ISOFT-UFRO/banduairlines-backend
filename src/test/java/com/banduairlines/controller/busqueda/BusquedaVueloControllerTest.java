package com.banduairlines.controller.busqueda;

import com.banduairlines.dto.busqueda.BusquedaItinerarioRequest;
import com.banduairlines.dto.busqueda.ClaseCabinaDisponibilidadResponse;
import com.banduairlines.dto.busqueda.ItinerarioVueloResponse;
import com.banduairlines.dto.busqueda.PasajerosBusquedaRequest;
import com.banduairlines.dto.busqueda.ProgramacionVueloRequest;
import com.banduairlines.dto.busqueda.ProgramacionVueloResponse;
import com.banduairlines.dto.busqueda.TramoVueloResponse;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.service.busqueda.BusquedaVueloService;
import com.banduairlines.service.busqueda.ItinerarioBusquedaService;
import com.banduairlines.service.busqueda.PasajerosBusquedaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BusquedaVueloControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BusquedaVueloService busquedaVueloService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ItinerarioBusquedaService itinerarioBusquedaService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PasajerosBusquedaService pasajerosBusquedaService;

    @InjectMocks
    private BusquedaVueloController busquedaVueloController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(busquedaVueloController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("RF-03: Buscar itinerarios directos y con escala retorna 200 OK")
    void buscarItinerarios_retornaListaDeAlternativas() throws Exception {
        TramoVueloResponse tramo = TramoVueloResponse.builder()
                .vueloId(45L)
                .codigoVuelo("BA-145")
                .origenCodigoIata("SCL")
                .origenCiudad("Santiago")
                .origenNombre("Arturo Merino Benítez")
                .destinoCodigoIata("PMC")
                .destinoCiudad("Puerto Montt")
                .destinoNombre("El Tepual")
                .salidaProgramada(LocalDateTime.of(2026, 11, 20, 9, 0))
                .llegadaProgramada(LocalDateTime.of(2026, 11, 20, 10, 50))
                .duracionMinutos(110)
                .aeronaveModelo("Airbus A320neo")
                .estadoVuelo("PROGRAMADO")
                .build();

        List<TramoVueloResponse> tramosList = new ArrayList<>();
        tramosList.add(tramo);

        ItinerarioVueloResponse itinerario = ItinerarioVueloResponse.builder()
                .tipoItinerario("DIRECTO")
                .cantidadEscalas(0)
                .horarioSalida(LocalDateTime.of(2026, 11, 20, 9, 0))
                .horarioLlegada(LocalDateTime.of(2026, 11, 20, 10, 50))
                .duracionTotalMinutos(110)
                .tramos(tramosList)
                .build();

        List<ItinerarioVueloResponse> itinerariosList = new ArrayList<>();
        itinerariosList.add(itinerario);

        when(itinerarioBusquedaService.buscar(any(BusquedaItinerarioRequest.class)).itinerarios())
                .thenReturn(itinerariosList);

        mockMvc.perform(get("/api/v1/busqueda/itinerarios")
                        .param("origen", "SCL")
                        .param("destino", "PMC")
                        .param("fecha", "2026-11-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoItinerario").value("DIRECTO"))
                .andExpect(jsonPath("$[0].cantidadEscalas").value(0))
                .andExpect(jsonPath("$[0].tramos[0].codigoVuelo").value("BA-145"));
    }

    @Test
    @DisplayName("RF-04: Obtener clases de cabina disponibles por vuelo retorna 200 OK")
    void obtenerDisponibilidadCabinas_retornaListaDeCabinas() throws Exception {
        List<String> packsList = new ArrayList<>();
        packsList.add("Light");
        packsList.add("Plus");

        ClaseCabinaDisponibilidadResponse cabinaDto = ClaseCabinaDisponibilidadResponse.builder()
                .vueloId(43L)
                .codigoVuelo("BA-143")
                .claseCabina("ECONOMY")
                .capacidadTotal(66)
                .asientosOcupados(0)
                .asientosDisponibles(66)
                .disponible(true)
                .precioDesde(new BigDecimal("29990.00"))
                .packsDisponibles(packsList)
                .build();

        List<ClaseCabinaDisponibilidadResponse> cabinasList = new ArrayList<>();
        cabinasList.add(cabinaDto);

        when(pasajerosBusquedaService.validarYResumir(any(PasajerosBusquedaRequest.class)).asientosNecesarios())
                .thenReturn(1L);
        when(busquedaVueloService.obtenerClasesCabinaDisponibles(eq(43L), anyLong()))
                .thenReturn(cabinasList);

        mockMvc.perform(get("/api/v1/busqueda/vuelos/43/disponibilidad")
                        .param("adultos", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vueloId").value(43))
                .andExpect(jsonPath("$[0].codigoVuelo").value("BA-143"))
                .andExpect(jsonPath("$[0].claseCabina").value("ECONOMY"))
                .andExpect(jsonPath("$[0].asientosDisponibles").value(66))
                .andExpect(jsonPath("$[0].disponible").value(true));
    }

    @Test
    @DisplayName("RF-09: Programar un nuevo vuelo retorna 201 Created")
    void programarVuelo_cuandoDatosValidos_retornaCreated() throws Exception {
        ProgramacionVueloRequest request = ProgramacionVueloRequest.builder()
                .codigoVuelo("BA-101")
                .rutaId(1L)
                .aeronaveId(1L)
                .salidaProgramada(LocalDateTime.now().plusDays(5))
                .llegadaProgramada(LocalDateTime.now().plusDays(5).plusHours(2))
                .build();

        ProgramacionVueloResponse response = ProgramacionVueloResponse.builder()
                .id(169L)
                .codigoVuelo("BA-101")
                .rutaId(1L)
                .origenCodigoIata("SCL")
                .destinoCodigoIata("ANF")
                .aeronaveId(1L)
                .aeronaveMatricula("CC-BAN")
                .aeronaveModelo("Airbus A320neo")
                .salidaProgramada(request.salidaProgramada())
                .llegadaProgramada(request.llegadaProgramada())
                .duracionMinutos(135)
                .estado("PROGRAMADO")
                .build();

        when(busquedaVueloService.programarVuelo(any(ProgramacionVueloRequest.class)))
                .thenReturn(response);

        String jsonBody = """
                {
                  "codigoVuelo": "BA-101",
                  "rutaId": 1,
                  "aeronaveId": 1,
                  "salidaProgramada": "2027-11-20T08:00:00",
                  "llegadaProgramada": "2027-11-20T10:15:00"
                }
                """;

        mockMvc.perform(post("/api/v1/busqueda/vuelos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(169))
                .andExpect(jsonPath("$.codigoVuelo").value("BA-101"))
                .andExpect(jsonPath("$.origenCodigoIata").value("SCL"))
                .andExpect(jsonPath("$.destinoCodigoIata").value("ANF"))
                .andExpect(jsonPath("$.estado").value("PROGRAMADO"));
    }

    @Test
    @DisplayName("RF-09: Modificar programación de vuelo existente retorna 200 OK")
    void modificarProgramacionVuelo_cuandoDatosValidos_retornaOk() throws Exception {
        ProgramacionVueloRequest request = ProgramacionVueloRequest.builder()
                .codigoVuelo("BA-101")
                .rutaId(1L)
                .aeronaveId(1L)
                .salidaProgramada(LocalDateTime.now().plusDays(5).plusHours(1))
                .llegadaProgramada(LocalDateTime.now().plusDays(5).plusHours(3))
                .build();

        ProgramacionVueloResponse response = ProgramacionVueloResponse.builder()
                .id(169L)
                .codigoVuelo("BA-101")
                .rutaId(1L)
                .origenCodigoIata("SCL")
                .destinoCodigoIata("ANF")
                .aeronaveId(1L)
                .aeronaveMatricula("CC-BAN")
                .aeronaveModelo("Airbus A320neo")
                .salidaProgramada(request.salidaProgramada())
                .llegadaProgramada(request.llegadaProgramada())
                .duracionMinutos(135)
                .estado("PROGRAMADO")
                .build();

        when(busquedaVueloService.modificarProgramacionVuelo(eq(169L), any(ProgramacionVueloRequest.class)))
                .thenReturn(response);

        String jsonBody = """
                {
                  "codigoVuelo": "BA-101",
                  "rutaId": 1,
                  "aeronaveId": 1,
                  "salidaProgramada": "2027-11-20T09:00:00",
                  "llegadaProgramada": "2027-11-20T11:15:00"
                }
                """;

        mockMvc.perform(put("/api/v1/busqueda/vuelos/169")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(169))
                .andExpect(jsonPath("$.codigoVuelo").value("BA-101"))
                .andExpect(jsonPath("$.duracionMinutos").value(135));
    }

    @Test
    @DisplayName("RF-09: Programar vuelo con solapamiento de aeronave retorna error de negocio")
    void programarVuelo_cuandoAeronaveSolapada_retornaErrorNegocio() throws Exception {
        when(busquedaVueloService.programarVuelo(any(ProgramacionVueloRequest.class)))
                .thenThrow(new ReglaNegocioException("La aeronave ya tiene otro vuelo asignado")
                        .conCodigo("AERONAVE_HORARIO_SOLAPADO"));

        String jsonBody = """
                {
                  "codigoVuelo": "BA-102",
                  "rutaId": 1,
                  "aeronaveId": 1,
                  "salidaProgramada": "2027-11-20T08:30:00",
                  "llegadaProgramada": "2027-11-20T10:45:00"
                }
                """;

        mockMvc.perform(post("/api/v1/busqueda/vuelos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().is4xxClientError());
    }
}