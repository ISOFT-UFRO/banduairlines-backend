package com.banduairlines.controller.reserva;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.reserva.PaginaReservasResponse;
import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.exception.ApiError;
import com.banduairlines.exception.DemasiadosIntentosException;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.service.reserva.ReservaHistorialService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// sirve a los endpoints 14 y 15.
// el endpoint 14 busca la info (código + correo) y en el 15 pasajero en jwt
// Los parámetros de buscar son required = false, si falta sale como 400 VALIDACION

@Validated
@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaHistorialController {

    private final ReservaHistorialService reservaHistorialService;

    public ReservaHistorialController(ReservaHistorialService reservaHistorialService) {
        this.reservaHistorialService = reservaHistorialService;
    }

    @GetMapping("/buscar")
    public ReservaResponse buscarReserva(
            @RequestParam(required = false)
            @NotBlank(message = "el codigo es obligatorio")
            @Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "el codigo debe tener 6 caracteres alfanuméricos")
            String codigo,

            @RequestParam(required = false)
            @NotBlank(message = "el correo es obligatorio")
            @Email(message = "el correo no tiene un formato válido")
            @Size(max = 150, message = "el correo no puede superar los 150 caracteres")
            String correo,

            HttpServletRequest http) {
        return reservaHistorialService.buscar(codigo, correo, http.getRemoteAddr());
    }

    @GetMapping("/mias")
    public PaginaReservasResponse listarMisReservas(
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return reservaHistorialService.listarMias(estado, pagina, tamano);
    }

    /**
     * 429 DEMASIADOS_INTENTOS del contrato (endpoint 14). Se responde aquí porque GlobalExceptionHandler
     * (exception/, de Integración) no conoce esta excepción y la respondería como 500.
     */
    @ExceptionHandler(DemasiadosIntentosException.class)
    public ResponseEntity<ApiError> demasiadosIntentos(DemasiadosIntentosException e, HttpServletRequest req) {
        HttpStatus estado = HttpStatus.TOO_MANY_REQUESTS;
        ApiError cuerpo = new ApiError(Instant.now(), estado.value(), estado.getReasonPhrase(),
                "DEMASIADOS_INTENTOS", e.getMessage(), req.getRequestURI(), List.of());
        return ResponseEntity.status(estado)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getReintentarEnSegundos()))
                .body(cuerpo);
    }
}
