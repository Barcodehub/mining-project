package com.mining.soft.application.service;

import java.util.List;

/**
 * Interpreta el valor crudo de un parámetro tal como llega de la fuente.
 *
 *  - número o texto numérico → VALUE (multiplicado por el factor de unidad)
 *  - "&lt;x" → CENSORED con LD = x·factor (bajo el límite de detección)
 *  - "&gt;x" → VALUE x·factor, marcado como censurado por arriba (cota inferior)
 *  - null, vacío o centinela de "no analizado" (p. ej. -999) → ABSENT
 *  - cualquier otro texto → UNPARSEABLE
 */
public final class MeasurementParser {

    public enum Kind { ABSENT, VALUE, CENSORED, UNPARSEABLE }

    public record Parsed(Kind kind, Double value, boolean rightCensored, String raw) {
        static Parsed absent() { return new Parsed(Kind.ABSENT, null, false, null); }
    }

    private MeasurementParser() { }

    public static Parsed parse(Object raw, double factor, List<Double> sentinels) {
        if (raw == null) return Parsed.absent();
        if (raw instanceof Number n) {
            double v = n.doubleValue();
            if (isSentinel(v, sentinels)) return Parsed.absent();
            return new Parsed(Kind.VALUE, v * factor, false, raw.toString());
        }
        String s = raw.toString().trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null") || s.equalsIgnoreCase("n.d.") || s.equals("-")) {
            return Parsed.absent();
        }
        boolean below = s.startsWith("<");
        boolean above = s.startsWith(">");
        String num = (below || above) ? s.substring(1).trim() : s;
        Double v = toDouble(num);
        if (v == null) return new Parsed(Kind.UNPARSEABLE, null, false, s);
        if (isSentinel(v, sentinels)) return Parsed.absent();
        if (below) return new Parsed(Kind.CENSORED, v * factor, false, s);
        return new Parsed(Kind.VALUE, v * factor, above, s);
    }

    /** Acepta "0.0005" y "0,0005" (coma decimal única). Rechaza separadores de miles ambiguos. */
    static Double toDouble(String s) {
        String t = s;
        long commas = t.chars().filter(c -> c == ',').count();
        long dots = t.chars().filter(c -> c == '.').count();
        if (commas == 1 && dots == 0) t = t.replace(',', '.');
        else if (commas > 0 || dots > 1) return null;
        try { return Double.parseDouble(t); }
        catch (NumberFormatException e) { return null; }
    }

    private static boolean isSentinel(double v, List<Double> sentinels) {
        if (sentinels == null) return false;
        for (Double s : sentinels) if (s != null && Double.compare(s, v) == 0) return true;
        return false;
    }
}
