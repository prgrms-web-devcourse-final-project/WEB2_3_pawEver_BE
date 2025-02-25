package com.pawever.server.domain.cicd.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;


public class RedisServiceTest {

    private static LettuceConnectionFactory redisConnectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static RedisService redisService;

    @BeforeAll
    static void setUp() {
        // ✅ 실제 Redis와 연결할 ConnectionFactory 생성
        redisConnectionFactory = new LettuceConnectionFactory("localhost", 6379);
        redisConnectionFactory.afterPropertiesSet();

        // ✅ RedisTemplate 초기화
        redisTemplate = new StringRedisTemplate(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();

        // ✅ RedisService 객체 직접 생성
        redisService = new RedisService(redisTemplate);
    }

    @AfterAll
    static void tearDown() {
        redisConnectionFactory.destroy(); // ✅ 테스트 후 Redis 연결 종료
    }

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
        System.out.println("✅ Redis Test without SpringBootTest & Mockito success");
    }
}