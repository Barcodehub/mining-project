package com.mining.soft.application.spi;
public interface StoragePort {
    String upload(String key, byte[] content, String contentType);
    byte[] download(String key);
    String getUrl(String key);
    void delete(String key);
    byte[] resizeToMiniature(byte[] content);
}