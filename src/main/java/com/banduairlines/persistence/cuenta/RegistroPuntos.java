package com.banduairlines.persistence.cuenta;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Immutable;
import java.time.Instant;

@Entity
@Table(name = "registros_puntos", uniqueConstraints = @UniqueConstraint(
        name = "uk_puntos_usuario_tipo_referencia", columnNames = {"usuario_id", "tipo", "referencia"}),
        indexes = @Index(name = "idx_puntos_usuario_fecha", columnList = "usuario_id,fecha"))
@Check(constraints = "cantidad > 0")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegistroPuntos {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRegistro;
    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long idUsuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", insertable = false, updatable = false)
    private com.banduairlines.model.Usuario usuario;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 20)
    private TipoRegistroPuntos tipo;
    @Column(nullable = false, updatable = false)
    private long cantidad;
    @Column(nullable = false, updatable = false)
    private Instant fecha;
    @Column(nullable = false, updatable = false, length = 120)
    private String referencia;
    @Column(name = "codigo_pnr", updatable = false, length = 6)
    private String codigoPNR;

    public RegistroPuntos(Long idUsuario, TipoRegistroPuntos tipo, long cantidad,
                          Instant fecha, String referencia, String codigoPNR) {
        if (idUsuario == null || tipo == null || cantidad <= 0 || fecha == null
                || referencia == null || referencia.isBlank() || referencia.length() > 120) {
            throw new IllegalArgumentException("Registro de puntos invalido");
        }
        this.idUsuario = idUsuario;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.fecha = fecha;
        this.referencia = referencia;
        this.codigoPNR = codigoPNR;
    }
}
