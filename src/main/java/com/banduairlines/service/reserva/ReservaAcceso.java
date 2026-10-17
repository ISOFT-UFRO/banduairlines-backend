package com.banduairlines.service.reserva;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.security.UsuarioAutenticado;

/**
 * Carga una reserva y aplica las reglas de acceso del contrato de RESERVA:
 * una reserva de pasajero registrado exige su JWT; una de invitado exige X-Compra-Token.
 *
 * La compra está "en curso" (se puede modificar) mientras la reserva está PENDIENTE_PAGO y PAGOS
 * todavía no registra ningún cobro para ella. EstadoReserva no tiene un valor propio para eso.
 */
@Component
public class ReservaAcceso {

    public static final String ESTADO_INVALIDO = "ESTADO_INVALIDO";
    public static final String RESERVA_EXPIRADA = "RESERVA_EXPIRADA";

    /** Estados de una compra todavía abierta (asientos bloqueados hasta expiraEn). */
    public static final Set<EstadoReserva> ABIERTAS = Set.of(EstadoReserva.PENDIENTE_PAGO);

    private final ReservaRepository reservaRepository;
    private final UsuarioActual usuarioActual;
    private final CompraTokenService compraTokenService;
    private final Clock clock;
    // Se resuelve al usarlo para no crear un ciclo de beans si la implementación de PAGOS usa ReservaAcceso.
    private final ObjectProvider<CobroReservaService> cobroReservaService;

    public ReservaAcceso(ReservaRepository reservaRepository, UsuarioActual usuarioActual,
                         CompraTokenService compraTokenService, Clock clock,
                         ObjectProvider<CobroReservaService> cobroReservaService) {
        this.reservaRepository = reservaRepository;
        this.usuarioActual = usuarioActual;
        this.compraTokenService = compraTokenService;
        this.clock = clock;
        this.cobroReservaService = cobroReservaService;
    }

    /** Devuelve la reserva si quien llama es su dueño (JWT) o trae su token de compra. */
    public Reserva obtenerAutorizada(Long reservaId, String compraToken) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reservaId));

        UsuarioAutenticado usuario = usuarioActual.obtener().orElse(null);
        if (usuario != null && usuario.rol() == RolUsuario.ADMINISTRADOR) {
            return reserva;
        }

        if (reserva.getUsuario() != null) {
            if (usuario == null) {
                throw new AuthenticationCredentialsNotFoundException("Debes iniciar sesión para acceder a esta reserva");
            }
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

    /** Para operaciones que arman la compra: la compra debe seguir en curso y sin vencer. */
    public void exigirEnCurso(Reserva reserva) {
        if (reserva.getEstado() != EstadoReserva.PENDIENTE_PAGO) {
            throw new ConflictoException("La reserva está " + reserva.getEstado() + " y ya no se puede modificar")
                    .conCodigo(ESTADO_INVALIDO);
        }
        if (tieneCobroIniciado(reserva)) {
            throw new ConflictoException("Ya se inició el pago de la reserva y ya no se puede modificar")
                    .conCodigo(ESTADO_INVALIDO);
        }
        exigirNoExpirada(reserva);
    }

    /** true si PAGOS ya registró un cobro para la reserva; desde ese momento la compra no se modifica. */
    public boolean tieneCobroIniciado(Reserva reserva) {
        return cobroReservaService.getObject().tieneCobroIniciado(reserva.getId());
    }

    public void exigirNoExpirada(Reserva reserva) {
        if (estaExpirada(reserva)) {
            throw new ConflictoException("Venció el tiempo para completar la compra")
                    .conCodigo(RESERVA_EXPIRADA);
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
