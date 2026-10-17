package com.banduairlines.dto.cuenta;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.text.Normalizer;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class SolicitudActualizacionPerfil {
    @Size(min = 1, max = 80)
    @Pattern(regexp = "\\p{L}[\\p{L}\\p{M} ]*", message = "Los nombres solo admiten letras Unicode y espacios")
    private String nombres;

    @Size(min = 1, max = 80)
    @Pattern(regexp = "\\p{L}[\\p{L}\\p{M} ]*", message = "Los apellidos solo admiten letras Unicode y espacios")
    private String apellidos;

    private boolean camposInvalidos;

    @JsonProperty("nombres")
    @Schema(type = "string", maxLength = 80)
    public void setNombres(Object valor) {
        if (valor instanceof String texto) {
            nombres = Normalizer.normalize(texto.strip(), Normalizer.Form.NFC);
        } else {
            camposInvalidos = true;
        }
    }

    @JsonProperty("apellidos")
    @Schema(type = "string", maxLength = 80)
    public void setApellidos(Object valor) {
        if (valor instanceof String texto) {
            apellidos = Normalizer.normalize(texto.strip(), Normalizer.Form.NFC);
        } else {
            camposInvalidos = true;
        }
    }

    @JsonAnySetter
    public void rechazarCampo(String campo, Object valor) {
        camposInvalidos = true;
    }

    @AssertTrue(message = "Debe enviar nombres o apellidos, como texto, sin otros campos")
    @Schema(hidden = true)
    public boolean isSolicitudValida() {
        return !camposInvalidos && (nombres != null || apellidos != null);
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }
}
