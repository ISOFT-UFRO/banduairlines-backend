package com.banduairlines.service.busqueda;

import com.banduairlines.dto.busqueda.ClaseCabinaDisponibilidadResponse;
import com.banduairlines.dto.busqueda.ItinerarioVueloResponse;
import com.banduairlines.dto.busqueda.ProgramacionVueloRequest;
import com.banduairlines.dto.busqueda.ProgramacionVueloResponse;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Aeropuerto;
import com.banduairlines.model.Avion;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.Ruta;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.Estado;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.repository.AsientoRepository;
import com.banduairlines.repository.AvionRepository;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.DisponibilidadBusquedaRepository;
import com.banduairlines.repository.RutaRepository;
import com.banduairlines.repository.TarifaVueloRepository;
import com.banduairlines.repository.VueloRepository;
import com.banduairlines.service.impl.BusquedaVueloServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusquedaVueloServiceTest {

    @Mock
    private VueloRepository vueloRepository;

    @Mock
    private TarifaVueloRepository tarifaVueloRepository;

    @Mock
    private AsientoRepository asientoRepository;

    @Mock
    private DisponibilidadBusquedaRepository disponibilidadRepository;

    @Mock
    private BoletoRepository boletoRepository;

    @Mock
    private RutaRepository rutaRepository;

    @Mock
    private AvionRepository avionRepository;

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneId.of("UTC"));

    @InjectMocks
    private BusquedaVueloServiceImpl busquedaVueloService;

    private Aeropuerto scl;
    private Aeropuerto anf;
    private Ruta rutaActiva;
    private Avion avionActivo;

    @BeforeEach
    void setUp() {
        scl = Aeropuerto.builder().id(1L).codigoIata("SCL").ciudad("Santiago").nombre("Arturo Merino Benítez").build();
        anf = Aeropuerto.builder().id(2L).codigoIata("ANF").ciudad("Antofagasta").nombre("Andrés Sabella").build();
        rutaActiva = Ruta.builder().id(1L).origen(scl).destino(anf).activa(true).build();
        avionActivo = Avion.builder().id(1L).matricula("CC-BAN").modelo("Airbus A320neo").estado(Estado.ACTIVA).build();
    }

    @Test
    @DisplayName("RF-03: Obtener alternativas de itinerario directo exitosamente")
    void obtenerAlternativasItinerario_cuandoHayVueloDirecto_retornaItinerarioDirecto() {
        LocalDate fecha = LocalDate.of(2026, 11, 20);
        Vuelo vueloDirecto = Vuelo.builder()
                .id(10L)
                .codigoVuelo("BA-101")
                .ruta(rutaActiva)
                .aeronave(avionActivo)
                .salidaProgramada(LocalDateTime.of(2026, 11, 20, 8, 0))
                .llegadaProgramada(LocalDateTime.of(2026, 11, 20, 10, 0))
                .estado(EstadoVuelo.PROGRAMADO)
                .build();

        List<Vuelo> vuelosDirectosList = new ArrayList<>();
        vuelosDirectosList.add(vueloDirecto);

        when(vueloRepository.buscarVuelosDirectos(eq("SCL"), eq("ANF"), any(), any(), anyCollection()))
                .thenReturn(vuelosDirectosList);
        when(vueloRepository.buscarPrimerosTramosParaConexion(eq("SCL"), eq("ANF"), any(), any(), anyCollection()))
                .thenReturn(new ArrayList<>());

        List<ItinerarioVueloResponse> resultado = busquedaVueloService.obtenerAlternativasItinerario("SCL", "ANF", fecha);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).tipoItinerario()).isEqualTo("DIRECTO");
        assertThat(resultado.get(0).duracionTotalMinutos()).isEqualTo(120);
    }

    @Test
    @DisplayName("RF-03: Lanza excepción cuando origen y destino son iguales")
    void obtenerAlternativasItinerario_cuandoOrigenIgualDestino_lanzaExcepcion() {
        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                busquedaVueloService.obtenerAlternativasItinerario("SCL", "SCL", LocalDate.of(2026, 11, 20));
            }
        });
        assertThat(excepcion.getMessage()).contains("origen y destino no pueden ser iguales");
    }

    @Test
    @DisplayName("RF-04: Calcula disponibilidad de clases de cabina correctamente")
    void obtenerClasesCabinaDisponibles_retornaDisponibilidadPorCabina() {
        Vuelo vuelo = Vuelo.builder()
                .id(43L)
                .codigoVuelo("BA-143")
                .ruta(rutaActiva)
                .aeronave(avionActivo)
                .salidaProgramada(LocalDateTime.of(2026, 11, 20, 10, 0))
                .llegadaProgramada(LocalDateTime.of(2026, 11, 20, 12, 0))
                .estado(EstadoVuelo.PROGRAMADO)
                .build();

        PackTarifario packLight = PackTarifario.builder().id(1L).nombre("Light").activo(true).build();
        TarifaVuelo tarifaEconomy = TarifaVuelo.builder()
                .id(1L)
                .vuelo(vuelo)
                .claseCabina(ClaseCabina.ECONOMY)
                .pack(packLight)
                .precio(new BigDecimal("29990.00"))
                .build();

        List<TarifaVuelo> tarifasList = new ArrayList<>();
        tarifasList.add(tarifaEconomy);

        when(vueloRepository.findByIdConDetalles(43L)).thenReturn(Optional.of(vuelo));
        when(tarifaVueloRepository.buscarTarifasActivasPorVuelo(43L)).thenReturn(tarifasList);
        when(asientoRepository.contarPorAeronaveYClaseCabina(1L, ClaseCabina.ECONOMY)).thenReturn(66L);
        lenient().when(disponibilidadRepository.contarPlazasOcupadas(eq(43L), eq(ClaseCabina.ECONOMY), any())).thenReturn(6L);
        lenient().when(boletoRepository.contarOcupadosPorVueloYClaseCabina(eq(43L), eq(ClaseCabina.ECONOMY), any())).thenReturn(6L);

        List<ClaseCabinaDisponibilidadResponse> cabinas = busquedaVueloService.obtenerClasesCabinaDisponibles(43L);

        assertThat(cabinas).hasSize(1);
        assertThat(cabinas.get(0).claseCabina()).isEqualTo("ECONOMY");
        assertThat(cabinas.get(0).asientosDisponibles()).isEqualTo(60);
        assertThat(cabinas.get(0).disponible()).isTrue();
    }

    @Test
    @DisplayName("RF-09: Programar vuelo exitosamente cuando ruta y aeronave están disponibles")
    void programarVuelo_cuandoDatosValidos_guardaYRetornaVuelo() {
        ProgramacionVueloRequest request = ProgramacionVueloRequest.builder()
                .codigoVuelo("ba-101")
                .rutaId(1L)
                .aeronaveId(1L)
                .salidaProgramada(LocalDateTime.of(2026, 11, 20, 8, 0))
                .llegadaProgramada(LocalDateTime.of(2026, 11, 20, 10, 15))
                .build();

        when(rutaRepository.findById(1L)).thenReturn(Optional.of(rutaActiva));
        when(avionRepository.findById(1L)).thenReturn(Optional.of(avionActivo));
        when(vueloRepository.existeSolapamientoAeronave(eq(1L), any(), any(), anyCollection(), eq(null)))
                .thenReturn(false);
        when(vueloRepository.existePorCodigoVueloYSalida(eq("BA-101"), any(), eq(null)))
                .thenReturn(false);
        when(vueloRepository.save(any(Vuelo.class))).thenAnswer(new Answer<Vuelo>() {
            @Override
            public Vuelo answer(InvocationOnMock invocation) throws Throwable {
                Vuelo v = invocation.getArgument(0);
                v.setId(169L);
                return v;
            }
        });

        ProgramacionVueloResponse response = busquedaVueloService.programarVuelo(request);

        assertThat(response.id()).isEqualTo(169L);
        assertThat(response.codigoVuelo()).isEqualTo("BA-101");
        assertThat(response.duracionMinutos()).isEqualTo(135);
        verify(vueloRepository).save(any(Vuelo.class));
    }

    @Test
    @DisplayName("RF-09: Rechaza programación si la aeronave está en mantenimiento")
    void programarVuelo_cuandoAeronaveEnMantenimiento_lanzaReglaNegocioException() {
        Avion avionMantenimiento = Avion.builder()
                .id(2L)
                .matricula("CC-MNT")
                .modelo("Airbus A320")
                .estado(Estado.MANTENIMIENTO)
                .build();

        ProgramacionVueloRequest request = ProgramacionVueloRequest.builder()
                .codigoVuelo("BA-200")
                .rutaId(1L)
                .aeronaveId(2L)
                .salidaProgramada(LocalDateTime.of(2026, 11, 20, 8, 0))
                .llegadaProgramada(LocalDateTime.of(2026, 11, 20, 10, 0))
                .build();

        when(rutaRepository.findById(1L)).thenReturn(Optional.of(rutaActiva));
        when(avionRepository.findById(2L)).thenReturn(Optional.of(avionMantenimiento));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                busquedaVueloService.programarVuelo(request);
            }
        });
        assertThat(excepcion.getMessage()).contains("no está disponible");
    }

    @Test
    @DisplayName("RF-09: Modificar programación de vuelo existente actualiza horarios correctamente")
    void modificarProgramacionVuelo_cuandoVueloProgramado_actualizaDatos() {
        Vuelo vueloExistente = Vuelo.builder()
                .id(169L)
                .codigoVuelo("BA-101")
                .ruta(rutaActiva)
                .aeronave(avionActivo)
                .salidaProgramada(LocalDateTime.of(2026, 11, 20, 8, 0))
                .llegadaProgramada(LocalDateTime.of(2026, 11, 20, 10, 15))
                .estado(EstadoVuelo.PROGRAMADO)
                .build();

        ProgramacionVueloRequest request = ProgramacionVueloRequest.builder()
                .codigoVuelo("BA-101")
                .rutaId(1L)
                .aeronaveId(1L)
                .salidaProgramada(LocalDateTime.of(2026, 11, 20, 9, 0))
                .llegadaProgramada(LocalDateTime.of(2026, 11, 20, 11, 15))
                .build();

        when(vueloRepository.findByIdConDetalles(169L)).thenReturn(Optional.of(vueloExistente));
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(rutaActiva));
        when(avionRepository.findById(1L)).thenReturn(Optional.of(avionActivo));
        when(vueloRepository.existeSolapamientoAeronave(eq(1L), any(), any(), anyCollection(), eq(169L)))
                .thenReturn(false);
        when(vueloRepository.existePorCodigoVueloYSalida(eq("BA-101"), any(), eq(169L)))
                .thenReturn(false);
        when(vueloRepository.save(any(Vuelo.class))).thenAnswer(new Answer<Vuelo>() {
            @Override
            public Vuelo answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0);
            }
        });

        ProgramacionVueloResponse response = busquedaVueloService.modificarProgramacionVuelo(169L, request);

        assertThat(response.salidaProgramada()).isEqualTo(LocalDateTime.of(2026, 11, 20, 9, 0));
        assertThat(response.llegadaProgramada()).isEqualTo(LocalDateTime.of(2026, 11, 20, 11, 15));
    }
}