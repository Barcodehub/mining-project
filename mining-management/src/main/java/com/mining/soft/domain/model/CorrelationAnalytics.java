package com.mining.soft.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CorrelationAnalytics {
    private double spearmanCoefficient;   // con corrección por empates
    @JsonProperty("pValue")
    private Double pValue;                // bilateral, por permutación
    private int clusters;                 // n
    private boolean exactPermutation;     // true si se enumeraron todas las permutaciones
    private long permutations;
}
