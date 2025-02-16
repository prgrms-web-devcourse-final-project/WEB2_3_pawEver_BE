package com.pawever.server.common.response;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ApiResponse {

    private Boolean isSuccess;
    private String status;
    private String code;
    private Object data;

    public static ApiResponse success(ResponseCodeEnum responseCodeEnum, Object data){
        return  new ApiResponse(true,responseCodeEnum.getStatus().name(),responseCodeEnum.getCode(),data);
    }
    public static ApiResponse success(ResponseCodeEnum responseCodeEnum){
        return success(responseCodeEnum,null);
    }
    public static ApiResponse fail(ResponseCodeEnum responseCodeEnum){
        return fail(responseCodeEnum,null);
    }

    public static ApiResponse fail(ResponseCodeEnum responseCodeEnum, Object data){
        return new ApiResponse(false,responseCodeEnum.getStatus().name(),responseCodeEnum.getCode(),data);
    }


    
}
