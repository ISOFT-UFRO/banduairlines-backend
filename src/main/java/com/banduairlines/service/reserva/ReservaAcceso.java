package com.banduairlines.service.reserva;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.security.UsuarioAutenticado;

/**
 * Carga una reserva y aplica las reglas de acceso del contrato de RESERVA:
 * una reserva de pasajero registrado exige su JWT; una de invitado exige X-Compra-Token
 * EstadoReserva no tiene EN_CURSO, asi que la compra abierta es PENDIENTE_PAGO hasta que vence expiraEn
 */
@Component
public class ReservaAcceso {

    public static final String ESTADO_INVALIDO = "ESTADO_INVALIDO";
    public static final String RESERVA_EXPIRADA = "RESERVA_EXPIRADA";

    /** Estados de una compra todavia abierta (asientos y cupo bloqueados hasta expiraEn). */
    public static final Set<EstadoReserva> ABIERTAS = Set.of(EstadoReserva.PENDIENTE_PAGO);

    private final ReservaRepository reservaRepository;
    private final UsuarioActual usuarioActual;
    private final CompraTokenService compraTokenService;
    private final Clock clock;

    public ReservaAcceso(ReservaRepository reservaRepository, UsuarioActual usuarioActual,
                         CompraTokenService compraTokenService, Clock clock) {
        this.reservaRepository = reservaRepository;
        this.usuarioActual = usuarioActual;
        this.compraTokenService = compraTokenService;
        this.clock = clock;
    }

    /** Devuelve la reserva si quien llama es su dueño (JWT) o trae su token de compra. 401, 403 o 404 si no */
    public Reserva obtenerAutorizada(Long reservaId, String compraToken) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reservaId));

        if (reserva.getUsuario() != null) {
            UsuarioAutenticado usuario = usuarioActual.obtener().orElseThrow(() ->
                    new AuthenticationCredentialsNotFoundException("Debes iniciar sesión para acceder a esta reserva"));
            if (!reserva.getUsuario().getId().equals(usuario.id())) {
                throw new AccessDeniedException("La reserva pertenece a otro usuario");
            }
            return reserva;
        }

        if (!compraTokenService.esValido(reserva.getId(), compraToken)) {
            throw new AuthenticationCredentialsNotFoundException("Falta el X-Compra-Token de la compra o no es válido");
        }
        return reserva;
    }

    /** Para operaciones que arman la compra: debe estar abierta y sin vencer */
    public void exigirAbierta(Reserva reserva) {
        if (!ABIERTAS.contains(reserva.getEstado())) {
            throw new ConflictoException("La reserva está " + reserva.getEstado() + " y ya no se puede modificar")
                    .conCodigo(ESTADO_INVALIDO);
        }
        if (estaExpirada(reserva)) {
            throw new ConflictoException("Venció el tiempo para completar la compra").conCodigo(RESERVA_EXPIRADA);
        }
    }

    public boolean estaExpirada(Reserva reserva) {
        return ABIERTAS.contains(reserva.getEstado())
                && reserva.getExpiraEn() != null
                && ahora().isAfter(reserva.getExpiraEn());
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
