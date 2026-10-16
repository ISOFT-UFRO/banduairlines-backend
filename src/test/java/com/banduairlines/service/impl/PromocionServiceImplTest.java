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

import com.banduairlines.dto.admin.PromocionRespuesta;
import com.banduairlines.dto.admin.PromocionSolicitud;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Promocion;
import com.banduairlines.model.enums.TipoDescuento;
import com.banduairlines.repository.PromocionRepository;

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

        PromocionRespuesta creada = promocionService.crear(solicitud());
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

        assertThat(promocionService.listar(true)).containsExactly(new PromocionRespuesta(ID, CODIGO,
                TipoDescuento.PORCENTAJE, VALOR, INICIO, EXPIRACION, USOS_MAXIMOS, USOS_ACTUALES, true));
    }

    @Test
    void actualizar_cuandoExiste_actualizaLosDatosYConservaLosUsosActuales() {
        Promocion existente = promocion();
        when(promocionRepository.findById(ID)).thenReturn(Optional.of(existente));
        when(promocionRepository.existsByCodigoAndIdNot(CODIGO_NUEVO, ID)).thenReturn(false);

        promocionService.actualizar(ID, new PromocionSolicitud(CODIGO_NUEVO, TipoDescuento.MONTO_FIJO,
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

    private PromocionSolicitud solicitud() {
        return solicitud(INICIO, EXPIRACION);
    }

    private PromocionSolicitud solicitud(LocalDateTime desde, LocalDateTime hasta) {
        return new PromocionSolicitud(CODIGO, TipoDescuento.PORCENTAJE, VALOR, desde, hasta, USOS_MAXIMOS);
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
