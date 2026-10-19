package com.banduairlines.service.reserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.banduairlines.model.Aeropuerto;
import com.banduairlines.model.Asiento;
import com.banduairlines.model.Avion;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Extra;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Ruta;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.TipoDocumento;
import com.banduairlines.model.enums.TipoExtra;
import com.banduairlines.model.enums.TipoPasajero;

/** Datos de prueba de RESERVA, endpoints 1 al 6 */
public final class CompraReservaFixtures {

    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 1, 18, 0);
    public static final LocalDateTime SALIDA = LocalDateTime.of(2026, 12, 10, 8, 30);

    private CompraReservaFixtures() {
    }

    public static PackTarifario pack(long id, String nombre, boolean incluyeAsiento) {
        return PackTarifario.builder().id(id).nombre(nombre)
                .incluyeEquipajeCabina(true).piezasBodegaIncluidas(0).kgPorPiezaBodega(23)
                .incluyeSeleccionAsiento(incluyeAsiento).incluyeEmbarquePrioritario(false).permiteCambios(false)
                .activo(true)
                .build();
    }

    public static PackTarifario packLight() {
        return pack(1L, "Light", false);
    }

    public static Avion avion() {
        return Avion.builder().id(5L).matricula("CC-BAA").modelo("A320").build();
    }

    public static Vuelo vuelo(long id, LocalDateTime salida) {
        Ruta ruta = Ruta.builder().id(3L)
                .origen(Aeropuerto.builder().id(1L).codigoIata("SCL").build())
                .destino(Aeropuerto.builder().id(2L).codigoIata("PMC").build())
                .build();
        return Vuelo.builder().id(id).codigoVuelo("BA" + id).ruta(ruta).aeronave(avion())
                .salidaProgramada(salida).llegadaProgramada(salida.plusHours(2))
                .estado(EstadoVuelo.PROGRAMADO)
                .build();
    }

    public static TarifaVuelo tarifa(Vuelo vuelo, ClaseCabina cabina, PackTarifario pack, long precio) {
        return TarifaVuelo.builder().id(vuelo.getId() * 10).vuelo(vuelo).claseCabina(cabina).pack(pack)
                .precio(BigDecimal.valueOf(precio)).build();
    }

    public static Reserva reserva(EstadoReserva estado) {
        return Reserva.builder().id(1L).codigoReserva("ABC234").emailContacto("")
                .estado(estado).subtotal(BigDecimal.ZERO).descuento(BigDecimal.ZERO).total(BigDecimal.ZERO)
                .expiraEn(AHORA.plusMinutes(10))
                .build();
    }

    public static Usuario usuario(long id) {
        return Usuario.builder().id(id).email("tomas@correo.cl").nombre("Tomás").apellido("Rojas")
                .telefono("+56911112222").passwordHash("x").build();
    }

    public static Pasajero pendiente(long id, Reserva reserva, TipoPasajero tipo) {
        Pasajero p = new PasajerosPendientes().crear(reserva, tipo, (int) id, SALIDA.toLocalDate());
        p.setId(id);
        return p;
    }

    public static Pasajero real(long id, Reserva reserva, TipoPasajero tipo, String nombre, LocalDate nacimiento) {
        return Pasajero.builder().id(id).reserva(reserva).tipo(tipo).nombre(nombre).apellido("Rojas")
                .tipoDocumento(TipoDocumento.RUT).numeroDocumento("DOC-" + id).fechaNacimiento(nacimiento)
                .build();
    }

    public static Boleto boleto(long id, Reserva reserva, Pasajero pasajero, TarifaVuelo tarifa, int orden) {
        return Boleto.builder().id(id).reserva(reserva).pasajeroId(pasajero).vueloId(tarifa.getVuelo())
                .tarifaVueloId(tarifa).ordenTramo(orden).precioTarifa(tarifa.getPrecio())
                .build();
    }

    /** Un boleto por pasajero en el tramo */
    public static List<Boleto> boletos(Reserva reserva, List<Pasajero> pasajeros, TarifaVuelo tarifa, int orden) {
        List<Boleto> boletos = new ArrayList<>();
        for (Pasajero p : pasajeros) {
            boletos.add(boleto(p.getId() * 100 + orden, reserva, p, tarifa, orden));
        }
        return boletos;
    }

    public static Asiento asiento(long id, int fila, String letra, ClaseCabina cabina, boolean preferente) {
        return Asiento.builder().id(id).aeronave(avion()).fila(fila).letra(letra).claseCabina(cabina)
                .esPreferente(preferente).build();
    }

    public static Extra extra(long id, TipoExtra tipo, long precio) {
        return Extra.builder().id(id).tipo(tipo).nombre(tipo.name()).precio(BigDecimal.valueOf(precio)).activo(true).build();
    }
}
