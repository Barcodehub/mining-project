package com.mining.soft.application.spi;
import java.time.LocalDateTime;
import java.util.Map;
public interface IPublishEventPort {
    void publish(String eventType, Long entityId, Long userId, Long targetUserId, LocalDateTime timestamp, Map<String, Object> metadata);
}
