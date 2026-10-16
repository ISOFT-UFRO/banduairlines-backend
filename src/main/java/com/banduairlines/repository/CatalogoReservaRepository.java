package com.banduairlines.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Asiento;
import com.banduairlines.model.Extra;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.TipoExtra;

import jakarta.persistence.LockModeType;

/**
 * Consultas de RESERVA sobre el catálogo de ADMIN y BÚSQUEDA (vuelos, tarifas, packs, asientos y extras).
 * Solo lectura: vive aparte para no modificar los repositorios de esos módulos.
 */
public interface CatalogoReservaRepository extends Repository<Vuelo, Long> {

    /** Bloquea la fila del vuelo hasta el fin de la transacción (SELECT ... FOR UPDATE). No lo modifica. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vuelo v where v.id = :id")
    Optional<Vuelo> bloquearVuelo(@Param("id") Long id);

    @Query("select p from PackTarifario p where lower(p.nombre) = lower(:nombre)")
    Optional<PackTarifario> buscarPackPorNombre(@Param("nombre") String nombre);

    @Query("select t from TarifaVuelo t where t.vuelo.id = :vueloId and t.claseCabina = :cabina and t.pack.id = :packId")
    Optional<TarifaVuelo> buscarTarifa(@Param("vueloId") Long vueloId,
                                       @Param("cabina") ClaseCabina cabina,
                                       @Param("packId") Long packId);

    @Query("select count(a) from Asiento a where a.aeronave.id = :aeronaveId and a.claseCabina = :cabina")
    long contarAsientos(@Param("aeronaveId") Long aeronaveId, @Param("cabina") ClaseCabina cabina);

    @Query("select a from Asiento a where a.aeronave.id = :aeronaveId order by a.fila, a.letra")
    List<Asiento> asientosDeAeronave(@Param("aeronaveId") Long aeronaveId);

    @Query("select e from Extra e where e.tipo = :tipo and e.activo = true order by e.id")
    List<Extra> extrasActivos(@Param("tipo") TipoExtra tipo);
}
