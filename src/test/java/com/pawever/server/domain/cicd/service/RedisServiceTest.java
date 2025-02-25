package com.pawever.server.domain.cicd.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RedisServiceTest {
    // Mock 객체 없이 실제 Redis 테스트

    @Autowired
    private RedisService redisService;

    @Test
    public void testSaveAndGetValue() {
        // given
        String key = "testKey";
        String value = "testValue";

        // when
        redisService.saveValue(key, value);
        String retrievedValue = redisService.getValue(key);

        // then
        assertThat(retrievedValue).isEqualTo(value);
        System.out.println("Redis Test without Mock success");
    }
}