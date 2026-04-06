package com.fraud.velocity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class VelocityTracker {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${fraud.rules.velocity-window-seconds}")
    private long velocityWindowSeconds;

    private static final String VELOCITY_KEY_PREFIX = "velocity:";
    private static final String DEST_KEY_PREFIX = "dest:";

    public long incrementAndGetTxnCount(String accountNumber) {
        String key = VELOCITY_KEY_PREFIX + accountNumber + ":txn_count";
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofSeconds(velocityWindowSeconds));
        log.debug("Velocity count for {}: {}", accountNumber, count);
        return count != null ? count : 0;
    }

    public long incrementAndGetDestCount(String sourceAccount, String destAccount) {
        String key = DEST_KEY_PREFIX + sourceAccount + ":" + destAccount + ":count";
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofSeconds(3600));
        return count != null ? count : 0;
    }

    public long getTxnCount(String accountNumber) {
        String key = VELOCITY_KEY_PREFIX + accountNumber + ":txn_count";
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }

    public void resetVelocity(String accountNumber) {
        String key = VELOCITY_KEY_PREFIX + accountNumber + ":txn_count";
        redisTemplate.delete(key);
    }
}
