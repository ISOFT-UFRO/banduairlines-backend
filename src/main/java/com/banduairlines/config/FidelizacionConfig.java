package com.banduairlines.config;

import com.banduairlines.service.cuenta.PropiedadesFidelizacion;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Activa la carga tipada del catálogo de beneficios desde la configuración. */
@Configuration
@EnableConfigurationProperties(PropiedadesFidelizacion.class)
public class FidelizacionConfig {
}
