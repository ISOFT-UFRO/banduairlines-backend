package com.banduairlines.service;

import com.banduairlines.exception.ValidacionException;
import com.banduairlines.service.cuenta.ReferenciaCanje;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenciaCanjeTest {

    @Test
    void creaReferenciaConClaveYBeneficio() {
        assertEquals("CANJE:clave-prueba-1:B3", ReferenciaCanje.crear("clave-prueba-1", 3L));
        assertEquals("CANJE:clave-prueba-1:", ReferenciaCanje.prefijo("clave-prueba-1"));
    }

    @Test
    void obtieneElBeneficioDeUnaReferenciaDeCanje() {
        assertEquals(Optional.of(3L), ReferenciaCanje.idBeneficio("CANJE:clave-prueba-1:B3"));
    }

    @Test
    void noObtieneBeneficioDeReferenciasQueNoSonCanjes() {
        assertTrue(ReferenciaCanje.idBeneficio("VUELO:15").isEmpty());
        assertTrue(ReferenciaCanje.idBeneficio(null).isEmpty());
        assertTrue(ReferenciaCanje.idBeneficio("CANJE:clave-prueba-1:Bxyz").isEmpty());
    }

    @Test
    void aceptaUnUuidYQuitaEspacios() {
        assertEquals("3f2b6c1e-8d4a-4f0e-9b7a-1c2d3e4f5a6b",
                ReferenciaCanje.validarClave("  3f2b6c1e-8d4a-4f0e-9b7a-1c2d3e4f5a6b "));
    }

    @Test
    void rechazaClaveAusente() {
        assertThrows(ValidacionException.class, () -> ReferenciaCanje.validarClave(null));
        assertThrows(ValidacionException.class, () -> ReferenciaCanje.validarClave("   "));
    }

    @Test
    void rechazaClaveConFormatoInvalido() {
        assertThrows(ValidacionException.class, () -> ReferenciaCanje.validarClave("corta"));
        assertThrows(ValidacionException.class, () -> ReferenciaCanje.validarClave("clave:con:dos-puntos"));
        assertThrows(ValidacionException.class, () -> ReferenciaCanje.validarClave("x".repeat(65)));
    }
}
