package com.pawever.server.domain.cicd.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@RestController
public class AppController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DataSource dataSource;

    @GetMapping("/redis")
    public String home() {
        // Redis에 값 저장
        redisTemplate.opsForValue().set("abc", "def");

        // Redis에서 값 가져오기
        String value = (String)redisTemplate.opsForValue().get("abc");

        return "Value from Redis: " + value;
    }

    @GetMapping("/dbpage")
    public String getDb() {
        String deptName = "";
        String deptno = "";
        String loc ="";
        try (Connection connection = dataSource.getConnection()) {
            // SQL 쿼리 실행
            String sql = "SELECT deptno, dname, loc FROM dept2 WHERE deptno = 11";
            try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

                // 결과 처리
                if (resultSet.next()) {
                    deptno = resultSet.getString("deptno");
                    deptName = resultSet.getString("dname");
                    loc = resultSet.getString("loc");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "Value from DB: " + deptno +" "+ deptName+" "+loc;
    }


    @GetMapping("/")
    public String hello() {
        return "pawEver 프로젝트 connection";
    }

    @GetMapping("/api/login")
    public String login() {
        return "login connected!";
    }

    @GetMapping("/search")
    public String search() {
        return "search connected!";
    }

}

