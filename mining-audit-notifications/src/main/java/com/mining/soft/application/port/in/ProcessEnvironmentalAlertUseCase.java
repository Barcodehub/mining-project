package com.mining.soft.application.port.in;

import com.mining.soft.domain.model.EnvironmentalAlert;

public interface ProcessEnvironmentalAlertUseCase {
    /**
     * Registra la alerta en la auditoría y notifica a los destinatarios configurados.
     * @return false si la alerta ya había sido procesada (entrega duplicada).
     */
    boolean process(EnvironmentalAlert alert);
}
