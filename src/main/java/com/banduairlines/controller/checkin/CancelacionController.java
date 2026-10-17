package com.banduairlines.controller.checkin;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.checkin.CancelacionInvitadoRequest;
import com.banduairlines.dto.checkin.CancelacionResponse;
import com.banduairlines.dto.checkin.CancelacionUsuarioRequest;
import com.banduairlines.dto.checkin.CondicionesCancelacionResponse;
import com.banduairlines.dto.checkin.CredencialesReservaRequest;
import com.banduairlines.dto.checkin.ReembolsoResponse;
import com.banduairlines.service.checkin.CancelacionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/checkin")
public class CancelacionController {

    private final CancelacionService cancelacionService;

    public CancelacionController(CancelacionService cancelacionService) {
        this.cancelacionService = cancelacionService;
    }

    @PostMapping("/invitado/cancelacion/condiciones")
    public CondicionesCancelacionResponse condicionesInvitado(@Valid @RequestBody CredencialesReservaRequest request){
        return cancelacionService.consultarCondicionesInvitado(request);
    }
    @PostMapping("/invitado/cancelacion")
    public CancelacionResponse cancelarInvitado(@Valid @RequestBody CancelacionInvitadoRequest request){
        return cancelacionService.cancelarInvitado(request);
    }
    @PostMapping("/invitado/reembolso")
    @ResponseStatus(HttpStatus.CREATED)
    public ReembolsoResponse reembolsoInvitado(@Valid @RequestBody CredencialesReservaRequest request){
        return cancelacionService.solicitarReembolsoInvitado(request);
    }

    @GetMapping("/reservas/{idReserva}/cancelacion")
    public CondicionesCancelacionResponse condicionesUsuario(@PathVariable Long idReserva){
        return cancelacionService.consultarCondicionesUsuario(idReserva);
    }
    @PostMapping("/reservas/{idReserva}/cancelacion")
    public CancelacionResponse cancelarUsuario(@PathVariable Long idReserva, @Valid @RequestBody(required = false)
                                               CancelacionUsuarioRequest request){
        return cancelacionService.cancelarUsuario(idReserva, request);
    }
    @PostMapping("/reservas/{idReserva}/reembolso")
    @ResponseStatus(HttpStatus.CREATED)
    public ReembolsoResponse reembolsoUsuario(@PathVariable Long idReserva){
        return cancelacionService.solicitarReembolsoUsuario(idReserva);
    }
}
