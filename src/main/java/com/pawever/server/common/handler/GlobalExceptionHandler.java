package com.pawever.server.common.handler;


import com.pawever.server.common.exception.CustomException;
import com.pawever.server.common.response.ApiResponse;
import com.pawever.server.common.response.ResponseCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Arrays;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //custom exception
    @ExceptionHandler({CustomException.class})
    public ResponseEntity<ApiResponse> handleCustomException(CustomException e){
        log.error("[{}] code:{} / code message:{}", e.getResponseCodeEnum().name(),e.getResponseCodeEnum().getCode(), e.getMessage());
        return ResponseEntity.status(e.getResponseCodeEnum().getStatus()).body(ApiResponse.fail(e.getResponseCodeEnum()));

    }

    //처리되지 못한 기타 exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handlingException(Exception e) {

        log.error("[Exception] CODE : {} |  MESSAGE : {}", ResponseCodeEnum.UNKNOWN_SERVER_ERROR.getCode(),e.getMessage());

        return ResponseEntity.status(ResponseCodeEnum.UNKNOWN_SERVER_ERROR.getStatus()).body(ApiResponse.fail(ResponseCodeEnum.UNKNOWN_SERVER_ERROR));

    }

    //@Valid 관련 에러
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleInvalidArgumentException(MethodArgumentNotValidException e){

        log.error("[Exception] CODE : {} |  MESSAGE : {}", ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getCode(), e.getMessage());
        return ResponseEntity.status(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getStatus()).body(ApiResponse.fail(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT));
    }


}
