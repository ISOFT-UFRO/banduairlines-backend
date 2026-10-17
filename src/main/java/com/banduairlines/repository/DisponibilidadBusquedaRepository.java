package com.banduairlines.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.banduairlines.model.enums.ClaseCabina;

import lombok.RequiredArgsConstructor;

/** Consultas de lectura sobre el inventario compartido con Reserva, sin modificar sus entidades. */
@Repository
@RequiredArgsConstructor
public class DisponibilidadBusquedaRepository {

    private static final String RESERVA_VIGENTE = """
              AND (r.estado = 'CONFIRMADA'
                   OR (r.estado IN ('EN_CURSO', 'PENDIENTE_PAGO')
                       AND (r.expira_en IS NULL OR r.expira_en > ?)))
            """;

    private final JdbcTemplate jdbcTemplate;

    public long contarPlazasOcupadas(Long vueloId, ClaseCabina cabina, LocalDateTime ahora) {
        // La base común aún no tiene TramoReserva. Cuando se integra Reserva, sus tramos
        // bloquean plazas incluso antes de crear pasajeros y boletos.
        boolean tieneTramos = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT to_regclass(current_schema() || '.tramos_reserva') IS NOT NULL", Boolean.class));

        String boletos = """
                SELECT COUNT(*) FROM boletos b
                JOIN pasajeros p ON p.id = b.pasajero_id
                JOIN reservas r ON r.id = b.reserva_id
                JOIN tarifas_vuelo t ON t.id = b.tarifa_vuelo_id
                WHERE b.vuelo_id = ? AND t.clase_cabina = ?
                  AND p.tipo IN ('ADULTO', 'NINO')
                """ + RESERVA_VIGENTE;
        if (tieneTramos) {
            // Un mismo grupo se cuenta por su tramo o por sus boletos, nunca por ambos.
            boletos += """
                      AND NOT EXISTS (SELECT 1 FROM tramos_reserva tr
                                      WHERE tr.reserva_id = b.reserva_id AND tr.vuelo_id = b.vuelo_id)
                    """;
        }
        long ocupadas = jdbcTemplate.queryForObject(boletos, Long.class,
                vueloId, cabina.name(), Timestamp.valueOf(ahora));
        if (!tieneTramos) {
            return ocupadas;
        }

        String tramos = """
                SELECT COALESCE(SUM(COALESCE(r.cantidad_adultos, 0) + COALESCE(r.cantidad_ninos, 0)), 0)
                FROM tramos_reserva tr
                JOIN reservas r ON r.id = tr.reserva_id
                JOIN tarifas_vuelo t ON t.id = tr.tarifa_vuelo_id
                WHERE tr.vuelo_id = ? AND t.clase_cabina = ?
                """ + RESERVA_VIGENTE;
        return ocupadas + jdbcTemplate.queryForObject(tramos, Long.class,
                vueloId, cabina.name(), Timestamp.valueOf(ahora));
    }
}
