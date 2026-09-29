package com.bank.transactionservice.service.impl;

import com.bank.transactionservice.dto.response.TransferResponse;
import com.bank.transactionservice.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl
        implements IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration TTL =
            Duration.ofHours(24);

    private static final Duration LOCK_TTL =
            Duration.ofMinutes(2);


    @Override
    public TransferResponse get(String key) {

        Object value =
                redisTemplate
                        .opsForValue()
                        .get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof String &&
                value.equals("PROCESSING")) {

            throw new IllegalStateException(
                    "A transfer with this Idempotency-Key is already being processed"
            );
        }

        return (TransferResponse) value;
    }


    @Override
    public void delete(String key) {

        redisTemplate.delete(key);
    }

    @Override
    public boolean acquireLock(String key) {

        Boolean acquired =
                redisTemplate
                        .opsForValue()
                        .setIfAbsent(
                                key,
                                "PROCESSING",
                                LOCK_TTL
                        );

        return Boolean.TRUE.equals(acquired);
    }


    @Override
    public void save(
            String key,
            TransferResponse response) {

        redisTemplate
                .opsForValue()
                .set(
                        key,
                        response,
                        TTL
                );
    }
}