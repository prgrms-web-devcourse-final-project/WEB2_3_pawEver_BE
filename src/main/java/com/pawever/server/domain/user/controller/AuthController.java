package com.pawever.server.domain.user.controller;

import com.pawever.server.common.response.ApiResponse;
import com.pawever.server.common.response.ResponseCodeEnum;
import com.pawever.server.domain.user.dto.request.AuthRequestDto;
import com.pawever.server.domain.user.dto.response.UserResponseDto;
import com.pawever.server.domain.user.jwt.JwtUtil;
import com.pawever.server.domain.user.service.AuthService;
import com.pawever.server.domain.user.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "인증 API", description = "JWT 토큰 발급(로그인) 및 재발급 API")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    @PostMapping("/tokens")
    @Operation(summary = "로그인 및 JWT 토큰(액세스/리프레시) 발급 API")
    public ResponseEntity<ApiResponse> login(@RequestBody AuthRequestDto authRequestDto) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(authService.login(authRequestDto))
            .body(ApiResponse.success(ResponseCodeEnum.SUCCESS));
    }

    @PostMapping("/refreshedtokens")
    @Operation(summary = "JWT 토큰(액세스/리프레시) 재발급 API")
    public ResponseEntity<ApiResponse> refreshTokens(HttpServletRequest request) {

        // 1. request로부터 유효한 refreshToken 가져오기
        // 쿠키나 Refresh 토큰이 없는 경우 400(BAD_REQUEST) 반환
        // 토큰 만료시 401(UNAUTHORIZED) 반환
        // Refresh 토큰이 아닌경우 400(BAD_REQUEST) 반환
        // 존재하지 않는 refresh 토큰이라면 탈취된 토큰으로 간주하고 401(UNAUTHORIZED) 반환
        String refreshToken = refreshTokenService.getValidRefreshToken(request);

        // 2. Refresh토큰의 사용자 정보 추출
        UserResponseDto userResponseDto = jwtUtil.getUserResponseDto(refreshToken);

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(authService.refreshTokens(userResponseDto, refreshToken))
            .body(ApiResponse.success(ResponseCodeEnum.SUCCESS));
    }

}
