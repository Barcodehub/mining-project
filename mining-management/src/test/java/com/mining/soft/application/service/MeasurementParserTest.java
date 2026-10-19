package com.mining.soft.application.service;

import com.mining.soft.application.service.MeasurementParser.Kind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MeasurementParserTest {

    static final List<Double> SENTINELS = List.of(-999.0);

    @Test
    void numbersAndNumericStrings() {
        assertEquals(7.71, MeasurementParser.parse("7.71", 1, SENTINELS).value());
        assertEquals(7.71, MeasurementParser.parse(7.71, 1, SENTINELS).value());
        assertEquals(1.9, MeasurementParser.parse("0.0019", 1000, SENTINELS).value(), 1e-12);
        assertEquals(0.039, MeasurementParser.parse("0,039", 1, SENTINELS).value(), 1e-12);
    }

    @Test
    void censoredValues() {
        MeasurementParser.Parsed p = MeasurementParser.parse("<0.0005", 1000, SENTINELS);
        assertEquals(Kind.CENSORED, p.kind());
        assertEquals(0.5, p.value(), 1e-12);
        assertEquals(Kind.CENSORED, MeasurementParser.parse("< 2.0", 1, SENTINELS).kind());
        MeasurementParser.Parsed above = MeasurementParser.parse(">4000", 1, SENTINELS);
        assertEquals(Kind.VALUE, above.kind());
        assertTrue(above.rightCensored());
    }

    @Test
    void absentAndUnparseable() {
        assertEquals(Kind.ABSENT, MeasurementParser.parse(null, 1, SENTINELS).kind());
        assertEquals(Kind.ABSENT, MeasurementParser.parse("-999", 1, SENTINELS).kind());
        assertEquals(Kind.ABSENT, MeasurementParser.parse(-999, 1, SENTINELS).kind());
        assertEquals(Kind.ABSENT, MeasurementParser.parse("  ", 1, SENTINELS).kind());
        // separadores de miles ambiguos (caudal "7.321.208" en la CDMB) no se interpretan
        assertEquals(Kind.UNPARSEABLE, MeasurementParser.parse("7.321.208", 1, SENTINELS).kind());
        assertEquals(Kind.UNPARSEABLE, MeasurementParser.parse("abc", 1, SENTINELS).kind());
    }
}
