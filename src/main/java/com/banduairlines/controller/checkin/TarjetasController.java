package com.banduairlines.controller.checkin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.checkin.TarjetaResponseDTO;
import com.banduairlines.service.checkin.TarjetasService;

@RestController
@RequestMapping("/api/v1/checkin")
public class TarjetasController {
    private TarjetasService tarjetasService;
    public TarjetasController(TarjetasService tarjetasService){
        this.tarjetasService = tarjetasService;
    }

    //GET checkin/reservas/{codigo_reserva}/tarjetas
    @GetMapping("/reservas/{codigo_reserva}/tarjetas")
    public ResponseEntity<List<TarjetaResponseDTO>> getTarjetas(@PathVariable("codigo_reserva") String codigoPnr){

        List<TarjetaResponseDTO> tarjetas = tarjetasService.getTarjetas(codigoPnr);

        if(tarjetas.isEmpty()){
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(tarjetas);
        }
    }


}
