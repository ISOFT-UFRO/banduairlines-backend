package com.banduairlines.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.admin.PromocionResponse;
import com.banduairlines.dto.admin.PromocionRequest;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Promocion;
import com.banduairlines.model.enums.TipoDescuento;
import com.banduairlines.repository.PromocionRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class PromocionServiceImplTest {

    private static final Long ID = 1L;
    private static final String CODIGO = "DESC10";
    private static final String CODIGO_NUEVO = "VERANO20";
    private static final BigDecimal VALOR = new BigDecimal("10.00");
    private static final BigDecimal VALOR_NUEVO = new BigDecimal("5000.00");
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 12, 1, 0, 0);
    private static final LocalDateTime EXPIRACION = LocalDateTime.of(2026, 12, 31, 23, 59, 59);
    private static final Integer USOS_MAXIMOS = 100;
    private static final Integer USOS_ACTUALES = 7;

    @Mock
    private PromocionRepository promocionRepository;

    @InjectMocks
    private PromocionServiceImpl promocionService;

    @Test
    void crear_cuandoElCodigoNoExiste_guardaLaPromocionActivaYConCeroUsos() {
        when(promocionRepository.existsByCodigo(CODIGO)).thenReturn(false);
        when(promocionRepository.save(any(Promocion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PromocionResponse creada = promocionService.crear(solicitud());
        assertThat(creada.codigo()).isEqualTo(CODIGO);
        assertThat(creada.valor()).isEqualTo(VALOR);
        assertThat(creada.activa()).isTrue();
        assertThat(creada.usosActuales()).isZero();
    }

    @Test
    void crear_cuandoElCodigoYaExiste_lanzaConflictoYNoGuarda() {
        when(promocionRepository.existsByCodigo(CODIGO)).thenReturn(true);

        assertThatThrownBy(() -> promocionService.crear(solicitud())).isInstanceOf(ConflictoException.class);

        verify(promocionRepository, never()).save(any(Promocion.class));
    }

    @Test
    void crear_cuandoLaVigenciaEsInvalida_lanzaReglaDeNegocio() {
        when(promocionRepository.existsByCodigo(CODIGO)).thenReturn(false);

        assertThatThrownBy(() -> promocionService.crear(solicitud(EXPIRACION, INICIO)))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        excepcion -> assertThat(excepcion.getCodigo()).isEqualTo("VIGENCIA_INVALIDA"));
    }

    @Test
    void listar_cuandoSeFiltraPorActivas_retornaLasPromocionesMapeadas() {
        when(promocionRepository.listarAdministracion(true)).thenReturn(List.of(promocion()));

        assertThat(promocionService.listar(true)).containsExactly(new PromocionResponse(ID, CODIGO,
                TipoDescuento.PORCENTAJE, VALOR, INICIO, EXPIRACION, USOS_MAXIMOS, USOS_ACTUALES, true));
    }

    @Test
    void actualizar_cuandoExiste_actualizaLosDatosYConservaLosUsosActuales() {
        Promocion existente = promocion();
        when(promocionRepository.findById(ID)).thenReturn(Optional.of(existente));
        when(promocionRepository.existsByCodigoAndIdNot(CODIGO_NUEVO, ID)).thenReturn(false);

        promocionService.actualizar(ID, new PromocionRequest(CODIGO_NUEVO, TipoDescuento.MONTO_FIJO,
                VALOR_NUEVO, INICIO, EXPIRACION, USOS_MAXIMOS));

        assertThat(existente.getCodigo()).isEqualTo(CODIGO_NUEVO);
        assertThat(existente.getValor()).isEqualTo(VALOR_NUEVO);
        assertThat(existente.getUsosActuales()).isEqualTo(USOS_ACTUALES);
    }

    @Test
    void cambiarEstado_cuandoSeDesactiva_dejaLaPromocionInactiva() {
        Promocion promocion = promocion();
        when(promocionRepository.findById(ID)).thenReturn(Optional.of(promocion));

        promocionService.cambiarEstado(ID, false);

        assertThat(promocion.getActiva()).isFalse();
    }

    @Test
    void crearRechazaPorcentajeSuperiorACien() {
        PromocionRequest solicitud = new PromocionRequest(
                CODIGO,
                TipoDescuento.PORCENTAJE,
                new BigDecimal("100.01"),
                INICIO,
                EXPIRACION,
                USOS_MAXIMOS);

        assertThatThrownBy(() -> promocionService.crear(solicitud))
                .isInstanceOfSatisfying(
                        ReglaNegocioException.class,
                        excepcion -> assertThat(excepcion.getCodigo())
                                .isEqualTo("DESCUENTO_PORCENTUAL_INVALIDO"));

        verify(promocionRepository, never()).save(any(Promocion.class));
    }

    @Test
    void actualizarRechazaPorcentajeSuperiorACienSinModificarLaPromocion() {
        Promocion existente = promocion();
        when(promocionRepository.findById(ID))
                .thenReturn(Optional.of(existente));

        PromocionRequest solicitud = new PromocionRequest(
                CODIGO_NUEVO,
                TipoDescuento.PORCENTAJE,
                new BigDecimal("100.01"),
                INICIO,
                EXPIRACION,
                USOS_MAXIMOS);

        assertThatThrownBy(() -> promocionService.actualizar(ID, solicitud))
                .isInstanceOfSatisfying(
                        ReglaNegocioException.class,
                        excepcion -> assertThat(excepcion.getCodigo())
                                .isEqualTo("DESCUENTO_PORCENTUAL_INVALIDO"));

        assertThat(existente.getCodigo()).isEqualTo(CODIGO);
        assertThat(existente.getValor()).isEqualByComparingTo(VALOR);
    }

    @ParameterizedTest
    @CsvSource({
            "PORCENTAJE, 100.00",
            "MONTO_FIJO, 5000.00"
    })
    void crearAceptaElLimitePorcentualYMontoFijo(
            TipoDescuento tipo, String valor) {

        BigDecimal importe = new BigDecimal(valor);

        when(promocionRepository.save(any(Promocion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        PromocionRequest solicitud = new PromocionRequest(
                CODIGO, tipo, importe, INICIO, EXPIRACION, USOS_MAXIMOS);

        PromocionResponse respuesta = promocionService.crear(solicitud);

        assertThat(respuesta.tipoDescuento()).isEqualTo(tipo);
        assertThat(respuesta.valor()).isEqualByComparingTo(importe);
    }

    @Test
    void actualizarAceptaPorcentajeDeCien() {
        Promocion existente = promocion();
        when(promocionRepository.findById(ID))
                .thenReturn(Optional.of(existente));

        PromocionRequest solicitud = new PromocionRequest(
                CODIGO_NUEVO,
                TipoDescuento.PORCENTAJE,
                new BigDecimal("100.00"),
                INICIO,
                EXPIRACION,
                USOS_MAXIMOS);

        PromocionResponse respuesta = promocionService.actualizar(ID, solicitud);

        assertThat(respuesta.valor())
                .isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(existente.getValor())
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }

    private PromocionRequest solicitud() {
        return solicitud(INICIO, EXPIRACION);
    }

    private PromocionRequest solicitud(LocalDateTime desde, LocalDateTime hasta) {
        return new PromocionRequest(CODIGO, TipoDescuento.PORCENTAJE, VALOR, desde, hasta, USOS_MAXIMOS);
    }

    private Promocion promocion() {
        return Promocion.builder()
                .id(ID)
                .codigo(CODIGO)
                .tipoDescuento(TipoDescuento.PORCENTAJE)
                .valor(VALOR)
                .vigenteDesde(INICIO)
                .vigenteHasta(EXPIRACION)
                .usosMaximos(USOS_MAXIMOS)
                .usosActuales(USOS_ACTUALES)
                .activa(true)
                .build();
    }
}
