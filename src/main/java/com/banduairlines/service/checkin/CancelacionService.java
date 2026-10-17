package com.banduairlines.service.checkin;

import com.banduairlines.dto.checkin.CancelacionInvitadoRequest;
import com.banduairlines.dto.checkin.CancelacionResponse;
import com.banduairlines.dto.checkin.CancelacionUsuarioRequest;
import com.banduairlines.dto.checkin.CondicionesCancelacionResponse;
import com.banduairlines.dto.checkin.CredencialesReservaRequest;
import com.banduairlines.dto.checkin.ReembolsoResponse;

public interface CancelacionService {

    CondicionesCancelacionResponse consultarCondicionesInvitado(CredencialesReservaRequest request);

    CondicionesCancelacionResponse consultarCondicionesUsuario(Long idReserva);

    CancelacionResponse cancelarInvitado(CancelacionInvitadoRequest request);

    CancelacionResponse cancelarUsuario(Long idReserva, CancelacionUsuarioRequest request);

    ReembolsoResponse solicitarReembolsoInvitado(CredencialesReservaRequest request);

    ReembolsoResponse solicitarReembolsoUsuario(Long idReserva);
}