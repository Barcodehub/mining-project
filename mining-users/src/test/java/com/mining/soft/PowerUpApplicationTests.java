package com.mining.soft;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/** Carga el contexto completo; requiere PostgreSQL (p. ej., el stack de docker compose): INTEGRATION_DB=true. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "INTEGRATION_DB", matches = "true")
class PowerUpApplicationTests {

    @Test
    void contextLoads() {
    }

}
