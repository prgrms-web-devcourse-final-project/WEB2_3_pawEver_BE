package com.pawever.server.common.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ResponseCodeEnum {

    // 성공 코드
    SUCCESS(HttpStatus.OK,"SUCCESS_0","요청에 성공"),
    CREATED(HttpStatus.CREATED,"SUCCESS_1","새로운 resource가 생성됨"),
    NO_CONTENT(HttpStatus.NO_CONTENT,"SUCCESS_2","반환할 응답값이 없음"),

    //요청관련 에러
    INVALID_REQUEST_ARGUMENT(HttpStatus.BAD_REQUEST,"REQUEST_0","부적절한 request argument가 전달됨"),


    // 인증 관련 에러
    JWT_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_0", "JWT 토큰이 만료되었습니다."),
    JWT_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_1", "JWT 토큰이 유효하지 않습니다."),
    INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN, "AUTH_2", "권한이 부족합니다."),

    // 사용자 관련 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_0", "user 정보를 찾을 수 없음."),


    //서버 에러
    UNKNOWN_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"SERVER_0","처리하지 못한 서버 내부 error 발생");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ResponseCodeEnum(HttpStatus status, String code, String message){
        this.status = status;
        this.code = code;
        this.message = message;

    }

}
