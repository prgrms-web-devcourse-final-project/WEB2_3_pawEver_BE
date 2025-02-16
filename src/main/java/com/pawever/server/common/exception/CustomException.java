package com.pawever.server.common.exception;

import com.pawever.server.common.response.ResponseCodeEnum;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException{

    private final ResponseCodeEnum responseCodeEnum ;

    public CustomException(ResponseCodeEnum code, String message) {
        super(message);
        this.responseCodeEnum = code;
    }

    public CustomException(ResponseCodeEnum code) {
        super(" ");
        this.responseCodeEnum = code;
    }


}