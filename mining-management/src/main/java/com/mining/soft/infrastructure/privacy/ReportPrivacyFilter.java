package com.mining.soft.infrastructure.privacy;

import com.mining.soft.infrastructure.input.dto.ReportResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Protección del reportante en vistas no privilegiadas: las coordenadas se generalizan al
 * centro de una celda de cuadrícula (por defecto 0.01°, ≈1.1 km) y se ocultan los
 * identificadores de usuario. Solo los roles de validación ven la ubicación exacta.
 */
@Component("reportPrivacy")
public class ReportPrivacyFilter {

    private final double gridDegrees;
    private final Set<String> preciseRoles;

    public ReportPrivacyFilter(@Value("${privacidad.reportes.cuadricula-grados:0.01}") double gridDegrees,
                               @Value("${privacidad.reportes.roles-precisos:ADMINISTRADOR,MODERADOR}") List<String> roles) {
        this.gridDegrees = gridDegrees;
        this.preciseRoles = roles.stream().map(r -> "ROLE_" + r.trim()).collect(Collectors.toSet());
    }

    public boolean privileged() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (preciseRoles.contains(a.getAuthority())) return true;
        }
        return false;
    }

    public ReportResponseDto apply(ReportResponseDto dto) {
        if (dto == null) return null;
        if (privileged()) {
            dto.setLocationPrecision("EXACT");
            return dto;
        }
        dto.setLatitude(snap(dto.getLatitude()));
        dto.setLongitude(snap(dto.getLongitude()));
        dto.setUserId(null);
        dto.setValidatedBy(null);
        dto.setLocationPrecision("GRID_" + gridDegrees + "_DEG");
        return dto;
    }

    public List<ReportResponseDto> apply(List<ReportResponseDto> list) {
        if (list == null) return null;
        list.forEach(this::apply);
        return list;
    }

    /** Centro de la celda que contiene la coordenada. */
    Double snap(Double v) {
        if (v == null) return null;
        double cell = Math.floor(v / gridDegrees) * gridDegrees + gridDegrees / 2.0;
        return Math.round(cell * 1e6) / 1e6;
    }
}
