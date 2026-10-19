package com.mining.soft.infrastructure.privacy;

import com.mining.soft.application.spi.IImageSanitizerPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Elimina metadatos (EXIF, incluida la ubicación GPS, XMP e IPTC) re-codificando la imagen
 * desde sus píxeles: ImageIO no copia segmentos de metadatos al escribir un BufferedImage.
 */
@Slf4j
@Component
public class ImageMetadataSanitizer implements IImageSanitizerPort {

    @Override
    public byte[] stripMetadata(byte[] content, String contentType) {
        String format = formatOf(contentType);
        if (format == null || content == null) return content;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(content));
            if (img == null) throw new IllegalArgumentException("No es una imagen decodificable");
            if ("jpg".equals(format) && img.getColorModel().hasAlpha()) {
                BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
                rgb.createGraphics().drawImage(img, 0, 0, null);
                img = rgb;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(img, format, out)) throw new IllegalArgumentException("Formato no escribible: " + format);
            return out.toByteArray();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("No se pudieron eliminar metadatos de la imagen ({}): {}", contentType, e.getMessage());
            throw new IllegalArgumentException("Imagen inválida o corrupta");
        }
    }

    private String formatOf(String contentType) {
        if (contentType == null) return null;
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            default -> null;
        };
    }
}
