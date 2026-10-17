package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.ActualizacionFidelizacion;
import com.banduairlines.dto.cuenta.MotivoBloqueo;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.cuenta.ActualizacionFidelizacionService;
import com.banduairlines.service.cuenta.BeneficioConfigurado;
import com.banduairlines.service.cuenta.CanjeService;
import com.banduairlines.service.cuenta.CatalogoBeneficios;
import com.banduairlines.service.cuenta.CodigosFidelizacion;
import com.banduairlines.service.cuenta.EvaluadorBeneficio;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos.CanjeRegistrado;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos.ResultadoRegistroCanje;
import com.banduairlines.service.cuenta.FuenteMovimientosPuntos.SolicitudRegistroCanje;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class CanjeServiceImpl implements CanjeService {

    static final int LARGO_MINIMO_CLAVE = 8;
    static final int LARGO_MAXIMO_CLAVE = 40;
    private static final Pattern FORMATO_CLAVE =
            Pattern.compile("[A-Za-z0-9-]{" + LARGO_MINIMO_CLAVE + "," + LARGO_MAXIMO_CLAVE + "}");

    private final AccesoMovimientosPuntos movimientos;
    private final CatalogoBeneficios catalogo;
    private final ActualizacionFidelizacionService actualizacion;
    private final EvaluadorBeneficio evaluador;
    private final UsuarioActual usuarioActual;
    private final MapeadorFidelizacion mapeador;

    public CanjeServiceImpl(AccesoMovimientosPuntos movimientos, CatalogoBeneficios catalogo,
                            ActualizacionFidelizacionService actualizacion, EvaluadorBeneficio evaluador,
                            UsuarioActual usuarioActual, MapeadorFidelizacion mapeador) {
        this.movimientos = movimientos;
        this.catalogo = catalogo;
        this.actualizacion = actualizacion;
        this.evaluador = evaluador;
        this.usuarioActual = usuarioActual;
        this.mapeador = mapeador;
    }

    /**
     * Valida el canje y calcula el descuento; el registro persistente y atómico lo hace {@link FuenteMovimientosPuntos}.
     * El estado de fidelización (saldo y nivel) viene de ActualizacionFidelizacionService, que también verifica que
     * la cuenta sea de un pasajero activo. No modifica millas ni nivel.
     */
    @Override
    @Transactional
    public ResultadoCanje canjearActual(Long idBeneficio, String claveIdempotencia) {
        Long idUsuario = usuarioActual.exigirId();
        if (idBeneficio == null || idBeneficio <= 0) {
            throw new ValidacionException("El idBeneficio debe ser un número positivo");
        }
        String clave = validarClave(claveIdempotencia);
        FuenteMovimientosPuntos fuente = movimientos.exigir();
        ActualizacionFidelizacion estado = actualizacion.actualizarActual();

        Optional<CanjeRegistrado> anterior = fuente.buscarCanje(idUsuario, clave);
        if (anterior.isPresent()) {
            return reintento(anterior.get(), idBeneficio);
        }

        BeneficioConfigurado beneficio = catalogo.buscar(idBeneficio).orElseThrow(() ->
                new RecursoNoEncontradoException("Beneficio", idBeneficio)
                        .conCodigo(CodigosFidelizacion.BENEFICIO_NO_ENCONTRADO));
        if (!beneficio.estaHabilitado()) {
            throw rechazo(MotivoBloqueo.BENEFICIO_AGOTADO);
        }

        long canjes = fuente.contarCanjes(Set.of(beneficio.id())).getOrDefault(beneficio.id(), 0L);
        Optional<MotivoBloqueo> motivo = evaluador.evaluar(
                beneficio, estado.nivelActual(), estado.saldoPuntos(), beneficio.stockInicial() - canjes);
        if (motivo.isPresent()) {
            throw rechazo(motivo.get());
        }

        long saldoAnterior = estado.saldoPuntos();
        long saldoPosterior = Math.subtractExact(saldoAnterior, beneficio.costoPuntos());
        ResultadoRegistroCanje resultado = fuente.registrarCanje(new SolicitudRegistroCanje(
                idUsuario, clave, beneficio.id(), beneficio.nombre(), beneficio.costoPuntos(),
                beneficio.stockInicial(), saldoAnterior, saldoPosterior));
        if (!resultado.creado()) {
            return reintento(resultado.canje(), idBeneficio);
        }
        return new ResultadoCanje(mapeador.canje(resultado.canje()), true);
    }

    /**
     * La misma clave con el mismo beneficio devuelve el canje original tal como quedó registrado (mismo idCanje,
     * saldos, fecha y nombre), sin consultar el catálogo actual ni descontar otra vez.
     * La misma clave con otro beneficio es un conflicto.
     */
    private ResultadoCanje reintento(CanjeRegistrado anterior, Long idBeneficio) {
        if (!idBeneficio.equals(anterior.movimiento().idBeneficio())) {
            throw new ConflictoException("La Clave-Idempotencia ya se usó para canjear otro beneficio")
                    .conCodigo(CodigosFidelizacion.CANJE_DUPLICADO);
        }
        return new ResultadoCanje(mapeador.canje(anterior), false);
    }

    /** Valida la cabecera Clave-Idempotencia y la devuelve sin espacios en los extremos. */
    static String validarClave(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new ValidacionException("El encabezado Clave-Idempotencia es obligatorio");
        }
        String limpia = clave.trim();
        if (!FORMATO_CLAVE.matcher(limpia).matches()) {
            throw new ValidacionException("La Clave-Idempotencia debe tener entre " + LARGO_MINIMO_CLAVE + " y "
                    + LARGO_MAXIMO_CLAVE + " caracteres: letras, números o guiones");
        }
        return limpia;
    }

    private static ReglaNegocioException rechazo(MotivoBloqueo motivo) {
        String mensaje = switch (motivo) {
            case BENEFICIO_AGOTADO -> "El beneficio no está disponible";
            case NIVEL_INSUFICIENTE -> "Tu nivel no alcanza para este beneficio";
            case PUNTOS_INSUFICIENTES -> "No tienes puntos suficientes para este beneficio";
        };
        return new ReglaNegocioException(mensaje).conCodigo(motivo.name());
    }
}
