package com.mining.soft.infrastructure.privacy;

import com.mining.soft.infrastructure.input.dto.ReportResponseDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Protección del reportante")
class PrivacyTest {

    final ReportPrivacyFilter filter = new ReportPrivacyFilter(0.01, List.of("ADMINISTRADOR", "MODERADOR"));

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private static void loginAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "u@x.co", null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private static ReportResponseDto report() {
        ReportResponseDto r = new ReportResponseDto();
        r.setLatitude(7.366712);
        r.setLongitude(-72.891734);
        r.setUserId(42L);
        r.setValidatedBy(7L);
        return r;
    }

    @Test
    @DisplayName("Rol ciudadano: coordenadas al centro de la celda de 0.01° y sin ids de usuario")
    void citizenGetsGeneralizedLocation() {
        loginAs("CLIENTE");
        ReportResponseDto r = filter.apply(report());
        assertEquals(7.365, r.getLatitude(), 1e-9);
        assertEquals(-72.895, r.getLongitude(), 1e-9);
        assertNull(r.getUserId());
        assertNull(r.getValidatedBy());
        assertEquals("GRID_0.01_DEG", r.getLocationPrecision());
    }

    @Test
    @DisplayName("Rol validador: ubicación exacta")
    void validatorGetsExactLocation() {
        loginAs("MODERADOR");
        ReportResponseDto r = filter.apply(report());
        assertEquals(7.366712, r.getLatitude());
        assertEquals(42L, r.getUserId());
        assertEquals("EXACT", r.getLocationPrecision());
    }

    @Test
    @DisplayName("Seudónimo estable, distinto por usuario y sin el id en claro")
    void pseudonym() {
        HmacPseudonymizer p = new HmacPseudonymizer("secreto-de-prueba");
        assertEquals(p.pseudonym(42L), p.pseudonym(42L));
        assertNotEquals(p.pseudonym(42L), p.pseudonym(43L));
        assertFalse(p.pseudonym(42L).contains("42"));
        assertNotEquals(p.pseudonym(42L), new HmacPseudonymizer("otra-clave").pseudonym(42L));
    }

    @Test
    @DisplayName("Se eliminan los metadatos EXIF/GPS de una foto JPEG")
    void exifIsStripped() throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        byte[] plain = out.toByteArray();

        // Inserta un segmento APP1 "Exif" con un marcador GPS justo después de SOI (FFD8).
        byte[] payload = "Exif\0\0GPSLatitude=7.3667;GPSLongitude=-72.8917".getBytes(StandardCharsets.ISO_8859_1);
        int len = payload.length + 2;
        ByteArrayOutputStream withExif = new ByteArrayOutputStream();
        withExif.write(plain, 0, 2);
        withExif.write(new byte[]{(byte) 0xFF, (byte) 0xE1, (byte) (len >> 8), (byte) len});
        withExif.write(payload);
        withExif.write(plain, 2, plain.length - 2);
        byte[] tagged = withExif.toByteArray();
        assertTrue(new String(tagged, StandardCharsets.ISO_8859_1).contains("GPSLatitude"));

        byte[] clean = new ImageMetadataSanitizer().stripMetadata(tagged, "image/jpeg");
        String s = new String(clean, StandardCharsets.ISO_8859_1);
        assertFalse(s.contains("Exif"));
        assertFalse(s.contains("GPSLatitude"));
        assertNotNull(ImageIO.read(new java.io.ByteArrayInputStream(clean)));
    }

    @Test
    @DisplayName("Contenido no imagen se deja intacto")
    void nonImageUntouched() {
        byte[] pdf = "%PDF-1.4".getBytes(StandardCharsets.US_ASCII);
        assertSame(pdf, new ImageMetadataSanitizer().stripMetadata(pdf, "application/pdf"));
    }
}
