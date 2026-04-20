package com.mining.soft.infrastructure.configuration;

import com.mining.soft.application.api.IEnvironmentalDataServicePort;
import com.mining.soft.application.spi.IEnvironmentalDataPersistencePort;
import com.mining.soft.application.usecase.EnvironmentalDataUseCase;
import com.mining.soft.infrastructure.out.jpa.adapter.EnvironmentalDataJpaAdapter;
import com.mining.soft.infrastructure.out.jpa.mapper.IEnvironmentalEntityMapper;
import com.mining.soft.infrastructure.out.jpa.repository.IAdapterRepository;
import com.mining.soft.infrastructure.out.jpa.repository.IEnvironmentalDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class EnvironmentalBeanConfiguration {

    private final IEnvironmentalDataRepository dataRepository;
    private final IAdapterRepository adapterRepository;
    private final IEnvironmentalEntityMapper entityMapper;

    @Bean
    public IEnvironmentalDataPersistencePort environmentalDataPersistencePort() {
        return new EnvironmentalDataJpaAdapter(dataRepository, adapterRepository, entityMapper);
    }

    @Bean
    public IEnvironmentalDataServicePort environmentalDataServicePort() {
        return new EnvironmentalDataUseCase(environmentalDataPersistencePort());
    }
}
