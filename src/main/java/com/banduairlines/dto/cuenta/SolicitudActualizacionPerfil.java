package com.banduairlines.dto.cuenta;

import com.banduairlines.model.enums.TipoDocumento;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.text.Normalizer;
import java.util.Locale;

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

    private TipoDocumento tipoDocumento;

    @Size(min = 1, max = 30)
    @Pattern(regexp = "[\\p{L}\\p{N}.-]+", message = "El numero de documento contiene caracteres no permitidos")
    private String numeroDocumento;

    private boolean camposInvalidos;
    private boolean tipoDocumentoEnviado;
    private boolean numeroDocumentoEnviado;

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

    @JsonProperty("tipoDocumento")
    @Schema(type = "string", allowableValues = {"RUT", "PASAPORTE", "OTRO"})
    public void setTipoDocumento(Object valor) {
        tipoDocumentoEnviado = true;
        if (valor instanceof String texto) {
            try {
                tipoDocumento = TipoDocumento.valueOf(texto.strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                camposInvalidos = true;
            }
        } else {
            camposInvalidos = true;
        }
    }

    @JsonProperty("numeroDocumento")
    @Schema(type = "string", maxLength = 30)
    public void setNumeroDocumento(Object valor) {
        numeroDocumentoEnviado = true;
        if (valor instanceof String texto) {
            numeroDocumento = texto.strip().toUpperCase(Locale.ROOT);
        } else {
            camposInvalidos = true;
        }
    }

    @JsonAnySetter
    public void rechazarCampo(String campo, Object valor) {
        camposInvalidos = true;
    }

    @AssertTrue(message = "Debe enviar nombres, apellidos o el documento completo, sin otros campos")
    @Schema(hidden = true)
    public boolean isSolicitudValida() {
        boolean hayDocumento = tipoDocumentoEnviado || numeroDocumentoEnviado;
        boolean documentoCompleto = !hayDocumento
                || (tipoDocumentoEnviado && numeroDocumentoEnviado
                && tipoDocumento != null && numeroDocumento != null && !numeroDocumento.isBlank());
        return !camposInvalidos && documentoCompleto
                && (nombres != null || apellidos != null || hayDocumento);
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }
}
