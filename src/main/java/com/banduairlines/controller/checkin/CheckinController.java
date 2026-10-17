package com.banduairlines.controller.checkin;

import com.banduairlines.dto.checkin.CheckinNoRegistradoDto;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Vuelo;
import com.banduairlines.service.checkin.CheckinService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/api/v1/checkin")
@RestController
public class CheckinController {
    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping("/checkin")
    public ResponseEntity<?> checkinNoRegistrado(@RequestBody CheckinNoRegistradoDto cnr) {
        Reserva reserva = checkinService.obtenerReserva(cnr.pnr());
        if (reserva == null) {
            throw new RecursoNoEncontradoException("No hay reservas registradas bajo estos datos");
        }
        boolean valido = checkinService.verificarApellidos(reserva, cnr.apellidos());
        if (valido) {
            Vuelo vuelo = checkinService.buscarVueloEnPeriodoCheckin(reserva);
            if (vuelo==null){
                throw new ReglaNegocioException("Fuera del periodo de Check-In, intente más tarde");
            }
            checkinService.realizarCheckin(cnr.pasajeros(),reserva,vuelo);
            return new ResponseEntity<>(HttpStatus.CREATED);
        }
        throw new RecursoNoEncontradoException("No hay reservas registradas bajo estos datos");
    }
    
}
