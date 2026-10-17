package com.banduairlines.controller.checkin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;

import com.banduairlines.dto.checkin.PeticionReservaQuery;
import com.banduairlines.dto.checkin.PeticionReservaResponse;
import com.banduairlines.service.checkin.ReservasService;

@RestController 
@RequestMapping("/api/v1/checkin/reservas")
public class ReservasController {

    private ReservasService reservasService;
    public ReservasController(ReservasService reservasService) {
        this.reservasService = reservasService;
    }


    @GetMapping
    public ResponseEntity<PeticionReservaResponse> getListado(){
        PeticionReservaResponse r = new PeticionReservaResponse("null"," null", "null", "null", "null"," null"," null"," null");
        return ResponseEntity.ok(r);
    }

    //Usuario NO registrado
    @PostMapping("/buscar")
    public ResponseEntity<PeticionReservaResponse> getReserva(@RequestBody PeticionReservaQuery dto){
        
        Optional<PeticionReservaResponse> response = reservasService.getReservaUsuarioNoRegistrado(dto);
        
        if(response.isEmpty()){
            return ResponseEntity.badRequest().body(null);
        } else {
            return ResponseEntity.ok(response.get());
        }
    }
}
