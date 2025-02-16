package com.pawever.server.config;
import groovy.util.logging.Slf4j;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TestDatabaseInitializer  {

    private static boolean initialized = false;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @Modifying
    public void setDatabase()  {
        //test 시 table 생성 & data 삽입
        //모든 test class에 대하여 한번만 실행되도록 함.
        if (!initialized) {
            entityManager.createNativeQuery("RUNSCRIPT FROM 'classpath:schema-test.sql'").executeUpdate();
            entityManager.createNativeQuery("RUNSCRIPT FROM 'classpath:data-test.sql'").executeUpdate();
            initialized = true;
        }
    }

}

