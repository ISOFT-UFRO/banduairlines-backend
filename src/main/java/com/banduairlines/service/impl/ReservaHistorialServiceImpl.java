package com.banduairlines.service.impl;
/* 
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.PaginaReservasResponse;
import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.dto.reserva.ReservaResumen;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.reserva.LimitadorIntentosBusqueda;
import com.banduairlines.service.reserva.ReservaConsultaService;
import com.banduairlines.service.reserva.ReservaHistorialService;

//relacionado a los endpoint 14 y 15 para el funcionaiento dle historial
@Service
public class ReservaHistorialServiceImpl implements ReservaHistorialService {

    static final int TAMANO_MAXIMO = 50;

    private final ReservaRepository reservaRepository;
    private final ReservaConsultaService reservaConsultaService;
    private final LimitadorIntentosBusqueda limitador;
    private final UsuarioActual usuarioActual;

    public ReservaHistorialServiceImpl(ReservaRepository reservaRepository,
                                       ReservaConsultaService reservaConsultaService,
                                       LimitadorIntentosBusqueda limitador,
                                       UsuarioActual usuarioActual) {
        this.reservaRepository = reservaRepository;
        this.reservaConsultaService = reservaConsultaService;
        this.limitador = limitador;
        this.usuarioActual = usuarioActual;
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResponse buscar(String codigo, String correo, String clienteIp) {
        String ip = clienteIp == null ? "desconocida" : clienteIp;
        String codigoNormalizado = codigo.trim().toUpperCase(Locale.ROOT);
        String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);

        limitador.exigirCupo(ip, codigoNormalizado, correoNormalizado);
        limitador.registrarIntento(ip);

        Optional<Reserva> reserva = reservaRepository.findByCodigoReserva(codigoNormalizado)
                .filter(r -> correoCoincide(r, correoNormalizado));
        if (reserva.isEmpty()) {
            limitador.registrarFallo(codigoNormalizado, correoNormalizado);
            throw new RecursoNoEncontradoException("No se encontró una reserva con esos datos");
        }
        return reservaConsultaService.armarRespuesta(reserva.get());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaReservasResponse listarMias(EstadoReserva estado, int pagina, int tamano) {
        Long usuarioId = usuarioActual.exigirId();
        int paginaEfectiva = Math.max(pagina, 0);
        int tamanoEfectivo = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        PageRequest pageable = PageRequest.of(paginaEfectiva, tamanoEfectivo);

        Page<Reserva> resultado = estado == null
                ? reservaRepository.findByUsuario_IdOrderByCreatedAtDescIdDesc(usuarioId, pageable)
                : reservaRepository.findByUsuario_IdAndEstadoOrderByCreatedAtDescIdDesc(usuarioId, estado, pageable);

        List<ReservaResumen> contenido = resultado.getContent().stream()
                .map(r -> resumir(reservaConsultaService.armarRespuesta(r)))
                .toList();
        return new PaginaReservasResponse(contenido, paginaEfectiva, tamanoEfectivo,
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    private static boolean correoCoincide(Reserva reserva, String correoNormalizado) {
        String correoReserva = reserva.getEmailContacto();
        return correoReserva != null && correoReserva.trim().equalsIgnoreCase(correoNormalizado);
    }

    private static ReservaResumen resumir(ReservaResponse r) {
        return new ReservaResumen(r.reservaId(), r.codigoReserva(), r.estado(), r.expiraEn(), r.pack(),
                r.cantidadPasajeros(), r.itinerario(), r.total());
    }
}
*/
