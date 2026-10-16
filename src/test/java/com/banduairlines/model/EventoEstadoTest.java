package com.banduairlines.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.hibernate.annotations.Immutable;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.CrudRepository;

import com.banduairlines.repository.EventoEstadoRepository;

class EventoEstadoTest {

    @Test
    void entidad_esInmutableYSinSetters() {
        assertThat(EventoEstado.class).hasAnnotation(Immutable.class);
        assertThat(Arrays.stream(EventoEstado.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .map(Method::getName))
                .noneMatch(nombre -> nombre.startsWith("set"));
    }

    @Test
    void repositorio_noExponeOperacionesDeBorrado() {
        assertThat(CrudRepository.class.isAssignableFrom(EventoEstadoRepository.class)).isFalse();
        assertThat(Arrays.stream(EventoEstadoRepository.class.getMethods()).map(Method::getName))
                .noneMatch(nombre -> nombre.startsWith("delete"));
    }
}
