package com.pawever.server.domain.cicd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisServiceTest {
    @Mock
    private RedisTemplate<String, String> redisTemplate; // RedisTemplate을 Mock 객체로 선언

    @Mock
    private ValueOperations<String, String> valueOperations; // ValueOperations을 Mock 객체로 선언

    @InjectMocks
    private RedisService redisService; // 테스트 대상 서비스

    @BeforeEach
    public void setUp() {
        // Mockito 초기화
        MockitoAnnotations.initMocks(this);

        // opsForValue()를 Mock 객체로 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    public void testSaveValue() {
        // Given: 테스트를 위한 값 설정
        String key = "myKey";
        String value = "myValue";

        // When: saveValue 메서드 호출
        redisService.saveValue(key, value);

        // Then: RedisTemplate의 opsForValue().set() 메서드가 호출되었는지 검증
        verify(valueOperations).set(key, value); // set() 메서드가 호출되었는지 검증
    }

    @Test
    public void testGetValue() {
        // Given: RedisTemplate이 리턴할 값을 설정
        String key = "myKey";
        String expectedValue = "myValue";
        when(valueOperations.get(key)).thenReturn(expectedValue); // mock 리턴 값 설정

        // When: getValue 메서드 호출
        String actualValue = redisService.getValue(key);

        // Then: 값이 제대로 반환되는지 검증
        assertEquals(expectedValue, actualValue); // 예상 값과 실제 값 비교
    }
}