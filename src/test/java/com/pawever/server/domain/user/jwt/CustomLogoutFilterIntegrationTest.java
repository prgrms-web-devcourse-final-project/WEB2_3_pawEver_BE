package com.pawever.server.domain.user.jwt;

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
import com.pawever.server.common.response.ResponseCodeEnum;
import com.pawever.server.domain.user.dto.response.UserResponseDto;
import com.pawever.server.domain.user.enums.Role;
import com.pawever.server.domain.user.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
class CustomLogoutFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

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

    // 요청 시도 - refreshtoken이 있는 경우
    private ResultActions getResultActionsForLogout(String refreshToken) throws Exception {
        return mockMvc.perform(
            RestDocumentationRequestBuilders
                .delete("/api/auth/tokens") // DELETE 요청 (로그아웃)
                .cookie(new Cookie("refresh", refreshToken)) // Refresh Token 포함
                .contentType(MediaType.APPLICATION_JSON)
        );
    }

    // 요청 시도 - refreshtoken이 없는 경우
    private ResultActions getResultActionsForRefreshToken() throws Exception {
        return mockMvc.perform(
            RestDocumentationRequestBuilders
                .post("/api/auth/refreshedtokens") // 쿠키 없이 요청
                .cookie(new Cookie("object", "object"))
                .contentType(MediaType.APPLICATION_JSON)
        );
    }

    // RestDocs 문서화 핸들러
    private RestDocumentationResultHandler getDocumentForLogout(String identifier) {
        return document(
            "api/auth/logout/" + identifier,
            preprocessRequest(prettyPrint(), modifyUris().scheme("https").host("yellowdog.p-e.kr").removePort()),
            preprocessResponse(prettyPrint(), getModifiedHeader()),
            responseCookies(
                cookieWithName("refresh").description("로그아웃 처리로 인해 기존 저장된 Refresh Token 쿠키를 삭제하기 위한 빈 쿠키 (HttpOnly, maxAge=0, path='/', secure=false)").optional()
            ),
            responseFields(responseFieldDescriptors), // 응답 필드
            resource(
                ResourceSnippetParameters.builder()
                    .tag("인증-auth")
                    .summary("로그아웃 API")
                    .description("서버에서 Refresh Token을 삭제하고, 쿠키를 초기화하는 로그아웃 API입니다.")
                    .responseFields(responseFieldDescriptors)
                    .build()
            )
        );
    }

    // 1. 로그아웃 성공 테스트(200)
    @Test
    @Transactional
    void logoutSuccessTest() throws Exception {
        // 1. Given: Refresh Token 생성 및 저장
        UserResponseDto userResponseDto = UserResponseDto.builder()
            .userId(100)
            .socialLoginUuid("800a8d7a-442d-4f5b-967e-2ed138f6e789")
            .name("LogoutTester")
            .role(Role.ROLE_USER)
            .build();

        String refreshToken = jwtUtil.createJwt("refresh", userResponseDto, 1000L * 60 * 60 * 24); // 24시간 유효

        // 2. 생성된 RefreshToken 서버 Redis 저장
        refreshTokenService.saveRefreshToken(refreshToken, userResponseDto.getName());

        // When
        ResultActions resultActions = getResultActionsForLogout(refreshToken);

        // Then
        resultActions.andExpect(status().isOk()) // 200 OK
            .andExpect(jsonPath("isSuccess").value(true))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.SUCCESS.getCode()))
            .andExpect(header().string("Set-Cookie", Matchers.containsString("refresh=;")))
            .andExpect(header().string("Set-Cookie", Matchers.containsString("Max-Age=0")))
            .andExpect(header().string("Set-Cookie", Matchers.containsString("HttpOnly")));

        // 문서화
        resultActions.andDo(getDocumentForLogout("success"));
    }

    // 2. 로그아웃 실패 테스트 - 리프레시토큰이 없는 경우 (400)
    @Test
    @Transactional
    void logoutFailNullTokenTest() throws Exception {
        // When
        ResultActions resultActions = getResultActionsForRefreshToken();

        // Then
        resultActions.andExpect(status().isBadRequest()) // 400 Bad Request (토큰 없음)
            .andExpect(jsonPath("isSuccess").value(false))
            .andExpect(jsonPath("status").value(ResponseCodeEnum.REFRESH_TOKEN_NULL.getStatus().name()))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.REFRESH_TOKEN_NULL.getCode()));

        // 문서화
        resultActions.andDo(getDocumentForLogout("failure-null-token"));
    }

    //  3. 로그아웃 실패 테스트 - 만료된 토큰(401)
    @Test
    @Transactional
    void logoutFailExpiredTokenTest() throws Exception {
        // Given: 존재하지 않는 토큰
        UserResponseDto userResponseDto = UserResponseDto.builder()
            .userId(200)
            .socialLoginUuid("900a8d7a-442d-4f5b-967e-2ed138f6e456")
            .name("EXPIREDTokenLogoutTester")
            .role(Role.ROLE_ADMIN)
            .build();

        String expiredTestRefreshToken = jwtUtil.createJwt("refresh", userResponseDto, 0L); // 만료된 토큰 생성

        // When
        ResultActions resultActions = getResultActionsForLogout(expiredTestRefreshToken);

        // Then
        resultActions.andExpect(status().isUnauthorized()) // 401 Unauthorized
            .andExpect(jsonPath("isSuccess").value(false))
            .andExpect(jsonPath("status").value(ResponseCodeEnum.JWT_TOKEN_EXPIRED.getStatus().name()))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.JWT_TOKEN_EXPIRED.getCode()));

        // 문서화
        resultActions.andDo(getDocumentForLogout("failure-expired"));
    }

    // 4. 로그아웃 실패 테스트 - accesstoken을 보낸 경우(400)
    @Test
    @Transactional
    void logoutFailCategoryMismatchTest() throws Exception {
        // Given
        // 1. 토큰 생성후 서버 Redis에 넣지 않음
        UserResponseDto userResponseDto = UserResponseDto.builder()
            .userId(300)
            .socialLoginUuid("445a8d7a-442d-4f5b-967e-2ed138f6e542")
            .name("MismatchTokenLogoutTester")
            .role(Role.ROLE_STAFF)
            .build();

        // 2. access 토큰 생성해서 request에 담아 보냄
        String categoryMistmatchTestLogoutToken = jwtUtil.createJwt("access", userResponseDto,
            1000L * 60 * 60 * 24);

        // When
        ResultActions resultActions = getResultActionsForLogout(categoryMistmatchTestLogoutToken);

        // Then
        resultActions.andExpect(status().isBadRequest()) // 400 응답 확인
            .andExpect(jsonPath("isSuccess").value(false))
            .andExpect(jsonPath("status").value(
                ResponseCodeEnum.TOKEN_CATEGORY_MISMATCH.getStatus().name()))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.TOKEN_CATEGORY_MISMATCH.getCode()));

        // Documentation
        resultActions.andDo(getDocumentForLogout("failure-mismatch"));
    }

    // 5. 로그아웃 실패 테스트 - 서버에 없는 토큰 (401)
    @Test
    @Transactional
    void  logoutFailStolenTokenTest() throws Exception {
        // Given
        // 1. 토큰 생성후 서버 Redis에 넣지 않음
        UserResponseDto userResponseDto = UserResponseDto.builder()
            .userId(400)
            .socialLoginUuid("300a8d7a-442d-4f5b-967e-2ed138f6e333")
            .name("StolenTokenLogoutTester")
            .role(Role.ROLE_STAFF)
            .build();

        String stolenTestLogoutToken = jwtUtil.createJwt("refresh", userResponseDto, 1000L * 60 * 60 * 24);

        // When
        ResultActions resultActions = getResultActionsForLogout(stolenTestLogoutToken);

        // Then
        resultActions.andExpect(status().isUnauthorized()) // 401 응답 확인
            .andExpect(jsonPath("isSuccess").value(false))
            .andExpect(jsonPath("status").value(ResponseCodeEnum.REFRESH_TOKEN_NOT_FOUND.getStatus().name()))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.REFRESH_TOKEN_NOT_FOUND.getCode()));

        // Documentation
        resultActions.andDo(getDocumentForLogout("failure-stolen"));
    }

}