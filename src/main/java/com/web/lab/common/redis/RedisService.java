package com.web.lab.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.TimeUnit;


@Service
public class RedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisService.class);

    @Value("${cache.ttl.default}")
    private long cache_ttl_default;

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public RedisService(ObjectMapper objectMapper, StringRedisTemplate redisTemplate) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    public void save(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value, cache_ttl_default, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis unavailable while saving key {}", key, e);
        }
    }

    public void save(String key, String value, long ttl, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
        } catch (Exception e) {
            log.warn("Redis unavailable while saving key {}", key, e);
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis unavailable while deleting key {}", key, e);
        }
    }

    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return;
            }

            redisTemplate.delete(keys);
        } catch (Exception e) {
            log.warn("Redis unavailable while deleting keys by pattern {}", pattern, e);
        }
    }

    public String get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis unavailable while reading key {}", key, e);
            return null;
        }
    }

    public void saveJson(String key, Object value) {
        try {
            save(key, objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize Redis value for key {}", key, e);
        }
    }

    public <T> T getJson(String key, Class<T> type) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize Redis value for key {}", key, e);
            return null;
        }
    }

    public <T> T getJson(String key, TypeReference<T> typeReference) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(value, typeReference);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize Redis value for key {}", key, e);
            return null;
        }
    }
}
