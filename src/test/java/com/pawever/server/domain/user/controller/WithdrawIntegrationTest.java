package com.pawever.server.domain.user.controller;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.restdocs.cookies.CookieDocumentation.responseCookies;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyUris;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.pawever.server.PawEverApplication;
import com.pawever.server.common.exception.CustomException;
import com.pawever.server.common.response.ResponseCodeEnum;
import com.pawever.server.domain.user.dto.request.AuthRequestDto;
import com.pawever.server.domain.user.dto.response.UserResponseDto;
import com.pawever.server.domain.user.enums.Role;
import com.pawever.server.domain.user.jwt.JwtUtil;
import com.pawever.server.domain.user.service.AuthService;
import com.pawever.server.domain.user.service.RefreshTokenService;
import com.pawever.server.domain.user.service.UserService;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.HeadersModifyingOperationPreprocessor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@SpringBootTest(classes = {PawEverApplication.class})
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class})
class WithdrawIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private AuthService authService;

    private HeadersModifyingOperationPreprocessor getModifiedHeader() {
        return modifyHeaders()
            .remove("X-Content-Type-Options")
            .remove("X-XSS-Protection")
            .remove("Cache-Control")
            .remove("Pragma")
            .remove("Expires")
            .remove("Content-Length");
    }

    // 응답 필드 설명 (성공/실패용)
    private final List<FieldDescriptor> responseFieldDescriptors = List.of(
        fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
        fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태 코드"),
        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 에러 코드"),
        fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("추가 데이터 (성공시에는 별도로 반환되지 않음)")
    );

    // 요청 시도
    private ResultActions getResultActionsForWithdraw(String refreshToken) throws Exception {
        return mockMvc.perform(
            RestDocumentationRequestBuilders
                .delete("/api/users/profiles") // DELETE 요청 (회원탈퇴)
                .cookie(new Cookie("refresh", refreshToken)) // Refresh Token 포함
                .contentType(MediaType.APPLICATION_JSON)
        );
    }

    // RestDocs 문서화 핸들러
    private RestDocumentationResultHandler getDocumentForWithdraw(String identifier) {
        return document(
            "api/users/withdraw/" + identifier,
            preprocessRequest(prettyPrint(), modifyUris().scheme("https").host("yellowdog.p-e.kr").removePort()),
            preprocessResponse(prettyPrint(), getModifiedHeader()),
            responseCookies(
                cookieWithName("refresh").description("회원탈퇴 처리로 인해 기존 저장된 Refresh Token 쿠키를 삭제하기 위한 빈 쿠키 (HttpOnly, maxAge=0, path='/', secure=false)").optional()
            ),
            responseFields(responseFieldDescriptors), // 응답 필드
            resource(
                ResourceSnippetParameters.builder()
                    .tag("유저-user")
                    .summary("회원탈퇴 API")
                    .description("회원탈퇴 API로 사용자 로그아웃 및 서버에서 사용자 정보를 제거합니다.")
                    .responseFields(responseFieldDescriptors)
                    .build()
            )
        );
    }

    // 1. 회원탈퇴 성공 테스트(200)
    @Test
    @Transactional
    void withdrawSuccessTest() throws Exception {
        // 1. Given: 사용자 회원가입
        AuthRequestDto authRequestDto = AuthRequestDto.builder()
            .socialLoginUuid("800a8d7a-442d-4f5b-967e-2ed138f6e789")
            .name("withdrawSuccessTester")
            .email("withdrawSuccessTester@gmail.com")
            .socialLoginProvider("kakao")
            .latitude(BigDecimal.valueOf(12.13123))
            .longitude(BigDecimal.valueOf(14.12312))
            .build();

        HttpHeaders loginResponseHttpHeaders = authService.login(authRequestDto);

        List<String> cookies = loginResponseHttpHeaders.get(HttpHeaders.SET_COOKIE);

        String refreshTokenCookie = cookies.stream()
            .filter(cookie->cookie.startsWith("refresh="))
            .findFirst()
            .orElseThrow(()-> new CustomException(ResponseCodeEnum.REFRESH_TOKEN_NULL));

        String refreshToken = refreshTokenCookie.split(";")[0].split("=")[1];


        // When
        ResultActions resultActions = getResultActionsForWithdraw(refreshToken);

        // Then
        resultActions.andExpect(status().isOk()) // 200 OK
            .andExpect(jsonPath("isSuccess").value(true))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.SUCCESS.getCode()))
            .andExpect(header().string("Set-Cookie", Matchers.containsString("refresh=;")))
            .andExpect(header().string("Set-Cookie", Matchers.containsString("Max-Age=0")))
            .andExpect(header().string("Set-Cookie", Matchers.containsString("HttpOnly")));

        // 문서화
        resultActions.andDo(getDocumentForWithdraw("success"));
    }

    // 2. 회원탈퇴 실패 테스트 - 회원이 서버 DB에 없는 경우 (404)
    @Test
    @Transactional
    void withdrawFailNullUserTest() throws Exception {
        // 1. Given: 리프레시 토큰 생성 및 저장
        UserResponseDto userResponseDto = UserResponseDto.builder()
            .userId(200)
            .socialLoginUuid("900a8d7a-442d-4f5b-967e-2ed138f6e456")
            .name("WithdrawFailNullUserTester")
            .role(Role.ROLE_USER)
            .build();

        String refreshToken = jwtUtil.createJwt("refresh", userResponseDto, 1000L * 60 * 60 * 24); // 24시간 유효

        // 2. 생성된 RefreshToken 서버 Redis 저장
        refreshTokenService.saveRefreshToken(refreshToken, userResponseDto.getName());

        // 서버 DB에 User 정보 저장 안하고 실행해서 404 오류 발생시키기

        // When
        ResultActions resultActions = getResultActionsForWithdraw(refreshToken);

        // Then
        resultActions.andExpect(status().isNotFound()) // 404 NOTFOUND
            .andExpect(jsonPath("isSuccess").value(false))
            .andExpect(jsonPath("status").value(ResponseCodeEnum.USER_NOT_FOUND.getStatus().name()))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.USER_NOT_FOUND.getCode()));

        // 문서화
        resultActions.andDo(getDocumentForWithdraw("fail-NullUser"));
    }

}