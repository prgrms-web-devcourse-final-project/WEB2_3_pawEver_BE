package com.pawever.server.domain.user.controller;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyUris;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestPartFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawever.server.PawEverApplication;
import com.pawever.server.common.response.ResponseCodeEnum;
import com.pawever.server.domain.post.service.ImageService;
import com.pawever.server.domain.user.dto.request.AuthRequestDto;
import com.pawever.server.domain.user.dto.request.UserProfileUpdateRequestDto;
import com.pawever.server.domain.user.dto.response.UserResponseDto;
import com.pawever.server.domain.user.enums.Role;
import com.pawever.server.domain.user.jwt.JwtUtil;
import com.pawever.server.domain.user.repository.jpa.UserRepository;
import com.pawever.server.domain.user.service.AuthService;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
class UpdateUserProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

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

    // 요청 필드 (RequestPart 'data' JSON)
    private final List<FieldDescriptor> requestFieldDescriptors = List.of(
        fieldWithPath("name").type(JsonFieldType.STRING).optional().description("변경할 유저 이름"),
        fieldWithPath("introduction").type(JsonFieldType.STRING).optional().description("변경할 자기소개")
    );

    // 응답 필드 (성공)
    private final List<FieldDescriptor> responseFieldDescriptors = List.of(
        fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
        fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태 코드"),
        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
        fieldWithPath("data").type(JsonFieldType.NULL).optional().description("성공 시 데이터 없음 (실패 시 null 반환)")
    );

    // 요청 시도 (Multipart)
    private ResultActions getResultActionsForUpdateUserProfile(
        String accessTokenWithBearer,
        UserProfileUpdateRequestDto userProfileUpdateRequestDto,
        MockMultipartFile profileImageFile
    ) throws Exception {
        return mockMvc.perform(
            RestDocumentationRequestBuilders.multipart("/api/users/profiles")
                .file(profileImageFile) // 이미지 파일
                .file(new MockMultipartFile(
                    "data", "", "application/json",
                    new ObjectMapper().writeValueAsBytes(userProfileUpdateRequestDto)
                )) // JSON 데이터
                .with(request -> { request.setMethod("PATCH"); return request; }) // PATCH 메서드로 강제
                .header(HttpHeaders.AUTHORIZATION, accessTokenWithBearer)
                .contentType(MediaType.MULTIPART_FORM_DATA)
        );
    }

    // RestDocs 문서화 핸들러
    private RestDocumentationResultHandler getDocumentForUpdateUserProfile(String identifier) {
        return document(
            "api/users/updateUserProfile/" + identifier,
            preprocessRequest(prettyPrint(), modifyUris().scheme("https").host("yellowdog.p-e.kr").removePort()),
            preprocessResponse(prettyPrint(), getModifiedHeader()),
            requestParts(
                partWithName("data").description("수정할 사용자 정보 (JSON)").optional(),
                partWithName("profileImage").description("변경할 프로필 이미지 파일 (선택)").optional()
            ),
            requestPartFields("data",
                (requestFieldDescriptors)
            ),
            responseFields(responseFieldDescriptors),
            resource(
                ResourceSnippetParameters.builder()
                    .tag("유저-user")
                    .summary("사용자(User) 프로필 수정 API")
                    .description("사용자의 이름, 자기소개, 프로필 이미지를 수정하는 API입니다.")
                    .responseFields(responseFieldDescriptors)
                    .build()
            )
        );
    }


    // 1. 유저 프로필 수정 성공 테스트
    @Test
    @Transactional
    void updateUserProfileSuccessTest() throws Exception {
        // Given: 로그인 및 토큰 발급
        AuthRequestDto authRequestDto = AuthRequestDto.builder()
            .socialLoginUuid("800a8d7a-442d-4f5b-967e-2ed138f6e789")
            .name("updateUserProfileSuccessTester")
            .email("updateUserProfileSuccessTester@example.com")
            .socialLoginProvider("kakao")
            .latitude(BigDecimal.valueOf(12.3456))
            .longitude(BigDecimal.valueOf(65.4321))
            .build();

        HttpHeaders loginResponseHttpHeaders = authService.login(authRequestDto);
        String accessTokenWithBearer = loginResponseHttpHeaders.getFirst(HttpHeaders.AUTHORIZATION);

        // 프로필 수정 요청 데이터
        UserProfileUpdateRequestDto updateRequestDto = UserProfileUpdateRequestDto.builder()
            .name("UpdatedTester")
            .introduction("안녕하세요, 업데이트된 자기소개입니다.")
            .build();

        // Mock 이미지 파일
        MockMultipartFile profileImageFile = new MockMultipartFile(
            "profileImage", "profile.jpg", "image/jpeg", "test-image-content".getBytes()
        );

        // When
        ResultActions resultActions = getResultActionsForUpdateUserProfile(accessTokenWithBearer, updateRequestDto, profileImageFile);

        // Then
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.code").value(ResponseCodeEnum.SUCCESS.getCode()));

        // 문서화
        resultActions.andDo(getDocumentForUpdateUserProfile("success"));

    }

    // 2. 유저 프로필 수정 실패 테스트 - 유저 정보 없음
    @Test
    @Transactional
    void updateUserProfileFailNullUserTest() throws Exception {
        // Given: accesstoken 발급
        UserResponseDto userResponseDto = UserResponseDto.builder()
            .userId(300)
            .socialLoginUuid("489a8d7a-442d-4f5b-967e-2ed138f6e321")
            .name("getUserProfileFailNullUserTester")
            .role(Role.ROLE_USER)
            .build();

        String accessTokenWithBearer = "Bearer " +jwtUtil.createJwt("access", userResponseDto, 1000L * 60 * 60 * 24); // 24시간 유효
        // 서버에 유저정보 저장 없이 업데이트 실행

        // 프로필 수정 요청 데이터
        UserProfileUpdateRequestDto updateRequestDto = UserProfileUpdateRequestDto.builder()
            .name("UpdatedTester")
            .introduction("안녕하세요, 업데이트된 자기소개입니다.")
            .build();

        // Mock 이미지 파일
        MockMultipartFile profileImageFile = new MockMultipartFile(
            "profileImage", "profile.jpg", "image/jpeg", "test-image-content".getBytes()
        );

        // When
        ResultActions resultActions = getResultActionsForUpdateUserProfile(accessTokenWithBearer, updateRequestDto, profileImageFile);

        // Then
        resultActions.andExpect(status().isNotFound()) // 404 NOTFOUND
            .andExpect(jsonPath("isSuccess").value(false))
            .andExpect(jsonPath("status").value(ResponseCodeEnum.USER_NOT_FOUND.getStatus().name()))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.USER_NOT_FOUND.getCode()));

        // 문서화
        resultActions.andDo(getDocumentForUpdateUserProfile("fail-NullUser"));

    }
}