package com.mining.soft.application.spi;

public interface IImageSanitizerPort {
    /**
     * Devuelve la imagen re-codificada sin metadatos (EXIF/GPS/XMP), o el contenido
     * original si no es un formato de imagen soportado.
     */
    byte[] stripMetadata(byte[] content, String contentType);
}
