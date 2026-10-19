package com.mining.soft.application.spi;

import com.mining.soft.domain.model.EnvironmentalAlertEvent;

public interface IAlertEventPort {
    /** Publica la alerta de forma asíncrona; no bloquea la respuesta de ingesta. */
    void publish(EnvironmentalAlertEvent event);
}
