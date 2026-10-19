package com.mining.soft.application.service;

import com.mining.soft.infrastructure.configuration.AmbientalConfig;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

/** Carga la política real (config-ambiental.yaml) tal como la enlaza Spring Boot. */
public final class PolicyFixture {

    private PolicyFixture() { }

    public static AmbientalConfig load() {
        try {
            List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                    .load("config-ambiental", new ClassPathResource("config-ambiental.yaml"));
            StandardEnvironment env = new StandardEnvironment();
            sources.forEach(s -> env.getPropertySources().addFirst(s));
            return new Binder(ConfigurationPropertySources.get(env))
                    .bind("ambiental", AmbientalConfig.class)
                    .orElseThrow(() -> new IllegalStateException("No se pudo enlazar 'ambiental'"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
