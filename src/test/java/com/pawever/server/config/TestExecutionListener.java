package com.pawever.server.config;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

//테스트 실행 전후에 특정 작업 실행하도록 하는 class
public class TestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestClass(TestContext testContext) {
        //모든 테스트 클래스 실행 전
        TestDatabaseInitializer initializer = testContext.getApplicationContext().getBean(TestDatabaseInitializer.class);
        initializer.setDatabase(); // 트랜잭션 적용된 메서드 호출

    }
}