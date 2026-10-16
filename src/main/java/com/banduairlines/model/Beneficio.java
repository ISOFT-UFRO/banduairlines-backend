package com.banduairlines.model;

import com.banduairlines.model.enums.Nivel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "beneficios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_beneficio")
    private Long idBeneficio;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_requerido", nullable = false, length = 20)
    private Nivel nivelRequerido;

    @Column(name = "costo_puntos", nullable = false)
    private Integer costoPuntos;

    @Column(nullable = false)
    @Builder.Default
    private boolean disponible = true;
}