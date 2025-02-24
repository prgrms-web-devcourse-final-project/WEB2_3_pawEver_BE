package com.pawever.server.domain.cicd.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

public class AlwaysFailTest {

    @Test
    public void testAlwaysFails() {
        // 이 테스트는 반드시 실패합니다.
        fail("이 테스트는 항상 실패합니다.");
    }
}
