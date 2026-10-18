package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.RespuestaRecuperacion;
import com.banduairlines.dto.cuenta.RespuestaValidacionRecuperacion;
import com.banduairlines.dto.cuenta.SolicitudNuevaContrasena;
import com.banduairlines.dto.cuenta.SolicitudRecuperacionContrasena;
import com.banduairlines.exception.ApiError;
import com.banduairlines.exception.CodigosError;
import com.banduairlines.exception.CodigoRecuperacionException;
import com.banduairlines.service.cuenta.RecuperacionContrasenaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/recuperaciones")
@Tag(
        name = "Cuentas",
        description = "Recuperacion de contrasena"
)
public class RecuperacionContrasenaController {

    private static final String MENSAJE_GENERICO =
            "Si el correo esta registrado, recibiras "
                    + "un enlace para recuperar tu contrasena";

    private final RecuperacionContrasenaService servicio;

    public RecuperacionContrasenaController(
            RecuperacionContrasenaService servicio) {

        this.servicio = servicio;
    }

    @PostMapping
    @Operation(
            summary = "Solicitar recuperacion de contrasena"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Solicitud recibida"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos invalidos"
            )
    })
    public ResponseEntity<RespuestaRecuperacion> solicitar(
            @Valid @RequestBody
            SolicitudRecuperacionContrasena solicitud) {

        servicio.solicitarRecuperacion(solicitud.correo());

        return ResponseEntity.accepted()
                .cacheControl(CacheControl.noStore())
                .body(new RespuestaRecuperacion(
                        MENSAJE_GENERICO
                ));
    }

    @GetMapping("/{codigoRecuperacion}")
    @Operation(
            summary = "Validar enlace de recuperacion"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Codigo vigente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Codigo invalido"
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "Codigo vencido o utilizado"
            )
    })
    public ResponseEntity<RespuestaValidacionRecuperacion>
    validar(
            @PathVariable String codigoRecuperacion) {

        servicio.validarRecuperacion(codigoRecuperacion);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        new RespuestaValidacionRecuperacion(true)
                );
    }

    @PutMapping("/{codigoRecuperacion}")
    @Operation(
            summary = "Restablecer contrasena"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Contrasena restablecida"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos o codigo invalidos"
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "Codigo vencido o utilizado"
            )
    })
    public ResponseEntity<Void> restablecer(
            @PathVariable String codigoRecuperacion,
            @Valid @RequestBody
            SolicitudNuevaContrasena solicitud) {

        servicio.restablecerContrasena(
                codigoRecuperacion,
                solicitud
        );

        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .build();
    }

    @ExceptionHandler(CodigoRecuperacionException.class)
    public ResponseEntity<ApiError> codigoInvalido(
            CodigoRecuperacionException ex) {

        String codigo;
        String mensaje;
        HttpStatus estadoHttp;

        switch (ex.getEstado()) {

            case VENCIDO -> {
                codigo = "CODIGO_RECUPERACION_VENCIDO";
                mensaje = "El codigo de recuperacion ha vencido";
                estadoHttp = HttpStatus.GONE;
            }

            case UTILIZADO -> {
                codigo = "CODIGO_RECUPERACION_UTILIZADO";
                mensaje = "El codigo de recuperacion ya fue utilizado";
                estadoHttp = HttpStatus.GONE;
            }

            default -> {
                codigo = "CODIGO_RECUPERACION_INVALIDO";
                mensaje = "El codigo de recuperacion no es valido";
                estadoHttp = HttpStatus.BAD_REQUEST;
            }
        }

        ApiError error = new ApiError(
                Instant.now(),
                estadoHttp.value(),
                estadoHttp.getReasonPhrase(),
                codigo,
                mensaje,
                "/api/v1/auth/recuperaciones/{codigoRecuperacion}",
                List.of()
        );

        return ResponseEntity
                .status(estadoHttp)
                .cacheControl(CacheControl.noStore())
                .body(error);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })

    public ResponseEntity<ApiError> datosInvalidos(
            Exception ex,
            HttpServletRequest request) {

        String ruta = request.getMethod().equals("POST")
                ? "/api/v1/auth/recuperaciones"
                : "/api/v1/auth/recuperaciones/{codigoRecuperacion}";

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                CodigosError.VALIDACION,
                "Los datos enviados no son validos",
                ruta,
                List.of()
        );

        return ResponseEntity.badRequest()
                .cacheControl(CacheControl.noStore())
                .body(error);
    }
}
