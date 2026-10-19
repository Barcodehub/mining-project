package com.mining.soft.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Correlación de Spearman con corrección por empates (Pearson sobre rangos promedio)
 * y p-valor bilateral por permutación: exacto para n ≤ 9, Monte Carlo (semilla fija) para n mayores.
 */
public final class SpearmanCorrelation {

    public record Result(double rho, Double pValue, int n, boolean exact, long permutations) { }

    static final int EXACT_MAX_N = 9;
    static final int MONTE_CARLO_PERMUTATIONS = 20_000;
    static final long SEED = 20261019L;

    private SpearmanCorrelation() { }

    public static Result compute(List<Double> x, List<Double> y) {
        int n = x.size();
        if (n != y.size()) throw new IllegalArgumentException("Tamaños distintos");
        if (n < 3) return new Result(Double.NaN, null, n, true, 0);
        double[] rx = ranks(x), ry = ranks(y);
        double rho = pearson(rx, ry);
        if (Double.isNaN(rho)) return new Result(Double.NaN, null, n, true, 0);

        double obs = Math.abs(rho) - 1e-12;
        if (n <= EXACT_MAX_N) {
            int[] idx = new int[n];
            for (int i = 0; i < n; i++) idx[i] = i;
            long[] counts = new long[2]; // [extremas, total]
            permute(idx, 0, rx, ry, obs, counts);
            return new Result(rho, (double) counts[0] / counts[1], n, true, counts[1]);
        }
        Random rnd = new Random(SEED);
        double[] perm = ry.clone();
        long extreme = 0;
        for (int k = 0; k < MONTE_CARLO_PERMUTATIONS; k++) {
            for (int i = n - 1; i > 0; i--) {
                int j = rnd.nextInt(i + 1);
                double t = perm[i]; perm[i] = perm[j]; perm[j] = t;
            }
            if (Math.abs(pearson(rx, perm)) >= obs) extreme++;
        }
        // Corrección (b+1)/(m+1) para no reportar p = 0 (Phipson & Smyth, 2010).
        double p = (extreme + 1.0) / (MONTE_CARLO_PERMUTATIONS + 1.0);
        return new Result(rho, p, n, false, MONTE_CARLO_PERMUTATIONS);
    }

    private static void permute(int[] idx, int k, double[] rx, double[] ry, double obs, long[] counts) {
        if (k == idx.length) {
            double[] p = new double[idx.length];
            for (int i = 0; i < idx.length; i++) p[i] = ry[idx[i]];
            if (Math.abs(pearson(rx, p)) >= obs) counts[0]++;
            counts[1]++;
            return;
        }
        for (int i = k; i < idx.length; i++) {
            swap(idx, k, i);
            permute(idx, k + 1, rx, ry, obs, counts);
            swap(idx, k, i);
        }
    }

    private static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    /** Rangos 1..n; los empates reciben el rango promedio. */
    static double[] ranks(List<Double> values) {
        int n = values.size();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) order.add(i);
        order.sort(Comparator.comparingDouble(values::get));
        double[] r = new double[n];
        int i = 0;
        while (i < n) {
            int j = i;
            while (j + 1 < n && Double.compare(values.get(order.get(j + 1)), values.get(order.get(i))) == 0) j++;
            double avg = (i + 1 + j + 1) / 2.0;
            for (int k = i; k <= j; k++) r[order.get(k)] = avg;
            i = j + 1;
        }
        return r;
    }

    static double pearson(double[] a, double[] b) {
        int n = a.length;
        double ma = 0, mb = 0;
        for (int i = 0; i < n; i++) { ma += a[i]; mb += b[i]; }
        ma /= n; mb /= n;
        double cov = 0, va = 0, vb = 0;
        for (int i = 0; i < n; i++) {
            double da = a[i] - ma, db = b[i] - mb;
            cov += da * db; va += da * da; vb += db * db;
        }
        if (va == 0 || vb == 0) return Double.NaN;
        return cov / Math.sqrt(va * vb);
    }
}
