package com.banduairlines.service.impl;

/*import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.security.UsuarioAutenticado;
import com.banduairlines.service.reserva.CompraTokenService;
import com.banduairlines.service.reserva.ReservaConsultaService;
import com.banduairlines.service.reserva.ReservaCuentaService;

//endpoint 16, falta comentario de explicar como se explica a profundida 
@Service
public class ReservaCuentaServiceImpl implements ReservaCuentaService {

    static final String YA_ASOCIADA = "YA_ASOCIADA";

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CompraTokenService compraTokenService;
    private final ReservaConsultaService reservaConsultaService;
    private final UsuarioActual usuarioActual;

    public ReservaCuentaServiceImpl(ReservaRepository reservaRepository,
                                    UsuarioRepository usuarioRepository,
                                    CompraTokenService compraTokenService,
                                    ReservaConsultaService reservaConsultaService,
                                    UsuarioActual usuarioActual) {
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.compraTokenService = compraTokenService;
        this.reservaConsultaService = reservaConsultaService;
        this.usuarioActual = usuarioActual;
    }

    @Override
    @Transactional
    public ReservaResponse asociar(Long reservaId, String compraToken) {
        UsuarioAutenticado usuario = usuarioActual.exigir();
        if (usuario.rol() != RolUsuario.PASAJERO) {
            throw new AccessDeniedException("Solo un pasajero puede asociar una reserva a su cuenta");
        }
        Reserva reserva = reservaRepository.findByIdForUpdate(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reservaId));

        if (!compraTokenService.esValido(reserva.getId(), compraToken)) {
            throw new AuthenticationCredentialsNotFoundException("Falta el X-Compra-Token de la compra o no es válido");
        }
        if (reserva.getUsuario() != null) {
            throw new ConflictoException("La reserva ya está asociada a una cuenta").conCodigo(YA_ASOCIADA);
        }

        reserva.setUsuario(usuarioRepository.getReferenceById(usuario.id()));
        reservaRepository.save(reserva);
        return reservaConsultaService.armarRespuesta(reserva);
    }
}*/