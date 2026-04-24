package com.mining.soft.infrastructure.input;

import com.mining.soft.application.api.IEnvironmentalDataServicePort;
import com.mining.soft.infrastructure.input.dto.AdapterRequestDto;
import com.mining.soft.infrastructure.input.mapper.IEnvironmentalDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/adapters")
@RequiredArgsConstructor
public class AdapterController {

    private final IEnvironmentalDataServicePort servicePort;
    private final IEnvironmentalDtoMapper mapper;

    @PostMapping
    @CacheEvict(value = "adapters", allEntries = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> createAdapter(@RequestBody AdapterRequestDto request) {
        servicePort.createAdapter(mapper.toAdapterModel(request));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
