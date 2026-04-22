package com.mining.soft.application.port.in;
import com.mining.soft.domain.model.ExternalEvent;
public interface ProcessEventUseCase {
    void process(ExternalEvent event);
}
