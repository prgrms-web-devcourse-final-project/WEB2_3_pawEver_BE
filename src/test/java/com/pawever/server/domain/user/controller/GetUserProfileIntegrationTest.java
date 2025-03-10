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
import com.pawever.server.domain.user.dto.request.UserProfileUpdateRequestDto;
import com.pawever.server.domain.user.dto.response.UserResponseDto;
import com.pawever.server.domain.user.entity.jpa.User;
import com.pawever.server.domain.user.enums.Role;
import com.pawever.server.domain.user.jwt.JwtUtil;
import com.pawever.server.domain.user.repository.jpa.UserRepository;
import com.pawever.server.domain.user.service.AuthService;
import com.pawever.server.domain.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
class GetUserProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    private HeadersModifyingOperationPreprocessor getModifiedHeader() {
        return modifyHeaders()
            .remove("X-Content-Type-Options")
            .remove("X-XSS-Protection")
            .remove("Cache-Control")
            .remove("Pragma")
            .remove("Expires")
            .remove("Content-Length");
    }

    // 응답 필드 설명 (성공용)
    private final List<FieldDescriptor> responseFieldDescriptorsForSuccess = List.of(
        fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
        fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태 코드"),
        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
        fieldWithPath("data.name").type(JsonFieldType.STRING).description("유저 이름"),
        fieldWithPath("data.email").type(JsonFieldType.STRING).description("유저 이메일"),
        fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).description("유저 프로필 이미지"),
        fieldWithPath("data.introduction").type(JsonFieldType.STRING).optional().description("유저 자기소개")
    );

    // 응답 필드 설명 (실패용)
    private final List<FieldDescriptor> responseFieldDescriptorsForFailure = List.of(
        fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
        fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태 코드"),
        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 에러 코드"),
        fieldWithPath("data").type(JsonFieldType.NULL).optional().description("실패 시 데이터 없음")
    );

    // 요청 시도
    private ResultActions getResultActionsForGetUserProfile(String accessTokenWithBearer) throws Exception {
        return mockMvc.perform(
            RestDocumentationRequestBuilders
                .get("/api/users/profiles") // GET 요청 (회원 프로필 조회)
                .header(HttpHeaders.AUTHORIZATION, accessTokenWithBearer) // Authorization 헤더로 전달
                .contentType(MediaType.APPLICATION_JSON)
        );
    }

    // 요청시도 - accesstoken이 없는 경우
    private ResultActions getResultActionsForGetUserProfile() throws Exception {
        return mockMvc.perform(
            RestDocumentationRequestBuilders
                .get("/api/users/profiles") // GET 요청 (회원 프로필 조회)
                .contentType(MediaType.APPLICATION_JSON)
        );
    }

    // RestDocs 문서화 핸들러
    private RestDocumentationResultHandler getDocumentForGetUserProfile(String identifier, Boolean isSuccess) throws Exception {
        return document(
            "api/users/getUserProfile/" + identifier,
            preprocessRequest(prettyPrint(), modifyUris().scheme("https").host("yellowdog.p-e.kr").removePort()),
            preprocessResponse(prettyPrint(), getModifiedHeader()),
            responseFields(isSuccess ? responseFieldDescriptorsForSuccess : responseFieldDescriptorsForFailure), // 응답 필드
            resource(
                ResourceSnippetParameters.builder()
                    .tag("유저-user")
                    .summary("사용자(User) 정보 조회 API")
                    .description("사용자의 이름, 이메일, 프로필이미지, 자기소개를 조회하는 API입니다.")
                    .responseFields(
                        isSuccess ? responseFieldDescriptorsForSuccess : responseFieldDescriptorsForFailure
                    )
                    .build()
            )
        );
    }

    // 1. 유저정보조회 성공 테스트 - 자기소개 없는 경우(200)
    @Test
    @Transactional
    void getUserProfileSuccessTest() throws Exception {
        // 1. Given: 사용자 회원가입
        AuthRequestDto authRequestDto = AuthRequestDto.builder()
            .socialLoginUuid("800a8d7a-442d-4f5b-967e-2ed138f6e789")
            .name("getUserProfileSuccessTester")
            .email("getUserProfileSuccessTester@gmail.com")
            .socialLoginProvider("kakao")
            .latitude(BigDecimal.valueOf(12.13123))
            .longitude(BigDecimal.valueOf(14.12312))
            .build();

        HttpHeaders loginResponseHttpHeaders = authService.login(authRequestDto);

        String accessTokenWithBearer = loginResponseHttpHeaders.getFirst(HttpHeaders.AUTHORIZATION);

        // When
        ResultActions resultActions = getResultActionsForGetUserProfile(accessTokenWithBearer);

        // Then
        resultActions.andExpect(status().isOk()) // 200 OK
            .andExpect(jsonPath("isSuccess").value(true))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.SUCCESS.getCode()))
            .andExpect(jsonPath("data.name").exists())
            .andExpect(jsonPath("data.email").exists())
            .andExpect(jsonPath("data.profileImageUrl").exists());

        // 문서화
        resultActions.andDo(getDocumentForGetUserProfile("success", true));
    }

    // 2. 유저정보조회 성공 테스트(200) - 자기소개 있는 경우
    @Test
    @Transactional
    void getUserProfileSuccessWithIntroductionTest() throws Exception {
        // 1. Given: 사용자 회원가입
        AuthRequestDto authRequestDto = AuthRequestDto.builder()
            .socialLoginUuid("900a8d7a-442d-4f5b-967e-2ed138f6e456")
            .name("getUserProfileWithIntroductionSuccessTester")
            .email("getUserProfileSuccessWithIntroductionTester@gmail.com")
            .socialLoginProvider("kakao")
            .latitude(BigDecimal.valueOf(12.13123))
            .longitude(BigDecimal.valueOf(14.12312))
            .build();

        HttpHeaders loginResponseHttpHeaders = authService.login(authRequestDto);

        String accessTokenWithBearer = loginResponseHttpHeaders.getFirst(HttpHeaders.AUTHORIZATION);

        // 2. 자기소개 업데이트
        User user = userRepository.findBySocialLoginUuid(authRequestDto.getSocialLoginUuid())
            .orElseThrow(()->new CustomException(ResponseCodeEnum.USER_NOT_FOUND));

        user.updateUserProfile(null, "안녕하세요? getUserProfileWithIntroductionSuccessTester입니다.");

        userRepository.saveAndFlush(user);

        // When
        ResultActions resultActions = getResultActionsForGetUserProfile(accessTokenWithBearer);

        // Then
        resultActions.andExpect(status().isOk()) // 200 OK
            .andExpect(jsonPath("isSuccess").value(true))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.SUCCESS.getCode()))
            .andExpect(jsonPath("data.name").exists())
            .andExpect(jsonPath("data.email").exists())
            .andExpect(jsonPath("data.profileImageUrl").exists())
            .andExpect(jsonPath("data.introduction").exists());

        // 문서화
        resultActions.andDo(getDocumentForGetUserProfile("success-with-Introduction", true));
    }

    // 3. 유저정보조회 실패 테스트(404) - 유저가 DB에 없는 경우
    @Test
    @Transactional
    void getUserProfileNullUserTest() throws Exception {
        // 1. Given: 사용자 회원가입
        UserResponseDto userResponseDto = UserResponseDto.builder()
            .userId(300)
            .socialLoginUuid("489a8d7a-442d-4f5b-967e-2ed138f6e321")
            .name("getUserProfileFailNullUserTester")
            .role(Role.ROLE_USER)
            .build();

        String accessTokenWithBearer = "Bearer " +jwtUtil.createJwt("access", userResponseDto, 1000L * 60 * 60 * 24); // 24시간 유효

        // 서버에 유저정보 저장 없이 업데이트 실행

        // When
        ResultActions resultActions = getResultActionsForGetUserProfile(accessTokenWithBearer);

        // Then
        resultActions.andExpect(status().isNotFound()) // 404 NOTFOUND
            .andExpect(jsonPath("isSuccess").value(false))
            .andExpect(jsonPath("status").value(ResponseCodeEnum.USER_NOT_FOUND.getStatus().name()))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.USER_NOT_FOUND.getCode()));

        // 문서화
        resultActions.andDo(getDocumentForGetUserProfile("fail-NullUser", false));
    }

    // 4. 유저정보조회 실패 테스트(400) - 액세스토큰이 없는 경우
    @Test
    @Transactional
    void getUserProfileNullTokenTest() throws Exception {
        // 1. Given: 액세스 토큰 없이 실행

        // When
        ResultActions resultActions = getResultActionsForGetUserProfile();

        // Then
        resultActions.andExpect(status().isBadRequest()) // 400 BAD REQUEST
            .andExpect(jsonPath("isSuccess").value(false))
            .andExpect(jsonPath("status").value(ResponseCodeEnum.ACCESS_TOKEN_NULL.getStatus().name()))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.ACCESS_TOKEN_NULL.getCode()));

        // 문서화
        resultActions.andDo(getDocumentForGetUserProfile("fail-NullToken", false));
    }

    // 5. 유저정보조회 실패 테스트(401) - 액세스토큰이 만료된 경우
    @Test
    @Transactional
    void getUserProfileExpiredTokenTest() throws Exception {
        // 1. Given: 만료된 액세스 토큰 생성
        UserResponseDto userResponseDto = UserResponseDto.builder()
            .userId(50)
            .socialLoginUuid("753a8d7a-442d-4f5b-967e-2ed138f6e123")
            .name("EXPIREDTokenGetUserProfileTester")
            .role(Role.ROLE_ADMIN)
            .build();

        String expiredTestAccessTokenWithBearer = "Bearer " +jwtUtil.createJwt("access", userResponseDto, 0L); // 만료된 토큰 생성

        // When
        ResultActions resultActions = getResultActionsForGetUserProfile(expiredTestAccessTokenWithBearer);

        // Then
        resultActions.andExpect(status().isUnauthorized()) // 401 Unauthorized
            .andExpect(jsonPath("isSuccess").value(false))
            .andExpect(jsonPath("status").value(ResponseCodeEnum.JWT_TOKEN_EXPIRED.getStatus().name()))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.JWT_TOKEN_EXPIRED.getCode()));

        // 문서화
        resultActions.andDo(getDocumentForGetUserProfile("fail-expiredToken", false));
    }

    // 6. 유저정보조회 실패 테스트(400) - 토큰타입이 액세스가 아닌 경우
    @Test
    @Transactional
    void getUserProfileTokenCategoryMismatchTest() throws Exception {
        // 1. Given: 만료된 액세스 토큰 생성
        UserResponseDto userResponseDto = UserResponseDto.builder()
            .userId(150)
            .socialLoginUuid("357a8d7a-442d-4f5b-967e-2ed138f6e321")
            .name("TokenCategoryMismatchGetUserProfileTester")
            .role(Role.ROLE_ADMIN)
            .build();

        String RefreshTokenWithBearer = "Bearer " +jwtUtil.createJwt("refresh", userResponseDto, 1000L * 60 * 60 * 24); // 24시간 유효

        // When
        ResultActions resultActions = getResultActionsForGetUserProfile(RefreshTokenWithBearer);

        // Then
        resultActions.andExpect(status().isBadRequest()) // 400 Badrequest
            .andExpect(jsonPath("isSuccess").value(false))
            .andExpect(jsonPath("status").value(ResponseCodeEnum.TOKEN_CATEGORY_MISMATCH.getStatus().name()))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.TOKEN_CATEGORY_MISMATCH.getCode()));

        // 문서화
        resultActions.andDo(getDocumentForGetUserProfile("fail-TokenCategoryMismatch", false));
    }

}