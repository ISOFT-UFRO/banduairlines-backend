package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.ActualizacionFidelizacion;
import com.banduairlines.dto.cuenta.ProgresoNivel;
import com.banduairlines.dto.cuenta.RespuestaBeneficio;
import com.banduairlines.dto.cuenta.RespuestaFidelizacion;
import com.banduairlines.dto.cuenta.RespuestaMovimientoPuntos;
import com.banduairlines.dto.cuenta.RespuestaPaginada;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.cuenta.ActualizacionFidelizacionService;
import com.banduairlines.service.cuenta.CatalogoBeneficios;
import com.banduairlines.service.cuenta.FidelizacionService;
import com.banduairlines.service.cuenta.GestionNivelService;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.service.cuenta.BeneficioConfigurado;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos.MovimientoPuntos;
import com.banduairlines.service.cuenta.RegistroPuntosService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class FidelizacionServiceImpl implements FidelizacionService {

    static final int MOVIMIENTOS_RECIENTES = 5;
    static final int TAMANO_MAXIMO_PAGINA = 100;

    private final ActualizacionFidelizacionService actualizacion;
    private final GestionNivelService niveles;
    private final CatalogoBeneficios catalogo;
    private final AccesoMovimientosPuntos movimientos;
    private final UsuarioRepository usuarios;
    private final UsuarioActual usuarioActual;
    private final MapeadorFidelizacion mapeador;

    public FidelizacionServiceImpl(ActualizacionFidelizacionService actualizacion, GestionNivelService niveles,
                                   CatalogoBeneficios catalogo, AccesoMovimientosPuntos movimientos,
                                   UsuarioRepository usuarios, UsuarioActual usuarioActual,
                                   MapeadorFidelizacion mapeador) {
        this.actualizacion = actualizacion;
        this.niveles = niveles;
        this.catalogo = catalogo;
        this.movimientos = movimientos;
        this.usuarios = usuarios;
        this.usuarioActual = usuarioActual;
        this.mapeador = mapeador;
    }

    /**
     * Saldo de puntos, millas, nivel y multiplicador vienen ya calculados por ActualizacionFidelizacionService, que
     * además valida que el usuario sea un pasajero activo. Aquí solo se transforman y se completan con el progreso,
     * los beneficios canjeables y los últimos movimientos.
     */
    @Override
    @Transactional
    public RespuestaFidelizacion consultarActual() {
        Long idUsuario = usuarioActual.exigirId();
        ActualizacionFidelizacion estado = actualizacion.actualizarActual();
        Nivel nivel = estado.nivelActual();
        long saldo = estado.saldoPuntos();

        List<BeneficioConfigurado> beneficios = catalogo.listar();
        List<RespuestaBeneficio> disponibles = mapeador.beneficios(beneficios, nivel, saldo,
                        CatalogoBeneficioServiceImpl.canjesRegistrados(beneficios, movimientos)).stream()
                .filter(RespuestaBeneficio::disponible)
                .toList();
        List<RespuestaMovimientoPuntos> recientes = mapeador.movimientos(movimientos.exigir()
                .consultarMovimientos(idUsuario, PageRequest.of(0, MOVIMIENTOS_RECIENTES)).getContent());

        return new RespuestaFidelizacion(
                saldo,
                estado.millasAcumuladas(),
                nivel,
                estado.multiplicadorPuntos(),
                progreso(estado.millasAcumuladas(), nivel),
                disponibles,
                recientes
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPaginada<RespuestaMovimientoPuntos> listarMovimientosActual(int pagina, int tamano) {
        if (pagina < 0) {
            throw new ValidacionException("La página no puede ser negativa");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO_PAGINA) {
            throw new ValidacionException("El tamaño de página debe estar entre 1 y " + TAMANO_MAXIMO_PAGINA);
        }
        Long idUsuario = usuarioActual.exigirId();
        Usuario usuario = usuarios.findById(idUsuario).orElseThrow(RegistroPuntosService::cuentaNoEncontrada);
        RegistroPuntosService.exigirPasajero(usuario);

        Page<MovimientoPuntos> resultado = movimientos.exigir()
                .consultarMovimientos(idUsuario, PageRequest.of(pagina, tamano));
        return new RespuestaPaginada<>(
                mapeador.movimientos(resultado.getContent()),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages()
        );
    }

    private ProgresoNivel progreso(BigDecimal millas, Nivel nivelActual) {
        Nivel siguiente = nivelSiguiente(nivelActual);
        if (siguiente == null) {
            return new ProgresoNivel(null, null, 0, 100);
        }
        long millasEnteras = millas.setScale(0, RoundingMode.DOWN).longValueExact();
        long minimoActual = niveles.obtenerConfiguracion(nivelActual).millasMinimas();
        long minimoSiguiente = niveles.obtenerConfiguracion(siguiente).millasMinimas();
        long faltantes = Math.max(0, minimoSiguiente - millasEnteras);
        long tramo = minimoSiguiente - minimoActual;
        int porcentaje = tramo <= 0
                ? 100
                : (int) Math.min(100, Math.max(0, (millasEnteras - minimoActual) * 100 / tramo));
        return new ProgresoNivel(siguiente, minimoSiguiente, faltantes, porcentaje);
    }

    /**
     * Orden de los niveles que gestiona Cuentas según el enum Nivel original
     * (BASICO, PLATA, ORO y PLATINO). PLATINO no tiene umbral configurado, así que ORO es el máximo.
     */
    private static Nivel nivelSiguiente(Nivel nivel) {
        return switch (nivel) {
            case BASICO -> Nivel.PLATA;
            case PLATA -> Nivel.ORO;
            case ORO, PLATINO -> null;
        };
    }
}
