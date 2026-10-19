package com.mining.soft.application.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpearmanCorrelationTest {

    @Test
    void v1TableWithTiesIsTieCorrected() {
        // SAI y CRI de la Tabla 3 del manuscrito v1 (C1, C2, C3, T1, N1, N2, N3)
        List<Double> sai = List.of(0.9409, 0.8556, 0.5800, 0.0833, 0.0, 0.0, 0.0);
        List<Double> cri = List.of(0.5608, 0.5118, 0.5256, 0.3450, 0.1836, 0.2914, 0.2200);
        SpearmanCorrelation.Result r = SpearmanCorrelation.compute(sai, cri);
        // Pearson sobre rangos promedio = 25/sqrt(26·28) = 0.92655, no 0.9286 (fórmula sin empates)
        assertEquals(25.0 / Math.sqrt(26 * 28), r.rho(), 1e-9);
        assertTrue(r.exact());
        assertEquals(5040, r.permutations());
        assertTrue(r.pValue() < 0.01);
    }

    @Test
    void perfectMonotoneRelationship() {
        List<Double> x = List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0);
        List<Double> y = List.of(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0);
        SpearmanCorrelation.Result r = SpearmanCorrelation.compute(x, y);
        assertEquals(1.0, r.rho(), 1e-12);
        // bilateral: solo la identidad y la inversa alcanzan |rho| = 1 → 2/7!
        assertEquals(2.0 / 5040.0, r.pValue(), 1e-12);
    }

    @Test
    void largeSampleUsesMonteCarloWithFixedSeed() {
        List<Double> x = List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0);
        List<Double> y = List.of(3.0, 1.0, 4.0, 1.5, 5.0, 9.0, 2.0, 6.0, 5.5, 3.5, 8.0, 9.5);
        SpearmanCorrelation.Result a = SpearmanCorrelation.compute(x, y);
        SpearmanCorrelation.Result b = SpearmanCorrelation.compute(x, y);
        assertFalse(a.exact());
        assertEquals(a.pValue(), b.pValue());   // reproducible
        assertTrue(a.pValue() > 0);
    }

    @Test
    void constantSeriesIsUndefined() {
        SpearmanCorrelation.Result r = SpearmanCorrelation.compute(
                List.of(0.0, 0.0, 0.0, 0.0), List.of(1.0, 2.0, 3.0, 4.0));
        assertTrue(Double.isNaN(r.rho()));
        assertNull(r.pValue());
    }
}
