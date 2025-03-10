package com.pawever.server.domain.user.controller;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyUris;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.pawever.server.PawEverApplication;
import com.pawever.server.common.response.ResponseCodeEnum;
import com.pawever.server.domain.carehub.entity.Shelter;
import com.pawever.server.domain.carehub.repository.ShelterRepository;
import com.pawever.server.domain.carehub.service.ShelterService;
import com.pawever.server.domain.user.dto.request.AuthRequestDto;
import com.pawever.server.domain.user.service.AuthService;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
class GetStaffProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private ShelterService shelterService;

    @Autowired
    private ShelterRepository shelterRepository;

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
        fieldWithPath("data[].userName").type(JsonFieldType.STRING).description("스태프 이름"),
        fieldWithPath("data[].userEmail").type(JsonFieldType.STRING).description("스태프 이메일"),
        fieldWithPath("data[].userProfileImageUrl").type(JsonFieldType.STRING).description("스태프 프로필 이미지"),
        fieldWithPath("data[].shelterName").type(JsonFieldType.STRING).description("보호소명"),
        fieldWithPath("data[].shelterCenterPhoneNumber").type(JsonFieldType.STRING).description("보호소 연락처"),
        fieldWithPath("data[].shelterManagerPhoneNumber").type(JsonFieldType.STRING).description("보호소 담당자 연락처")
    );

    // 응답 필드 설명 (실패용)
    private final List<FieldDescriptor> responseFieldDescriptorsForFailure = List.of(
        fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
        fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태 코드"),
        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 에러 코드"),
        fieldWithPath("data").type(JsonFieldType.NULL).optional().description("실패 시 데이터 없음(null)")
    );

    // 요청 시도
    private ResultActions getResultActionsForGetStaffProfile(String accessTokenWithBearer) throws Exception {
        return mockMvc.perform(
            RestDocumentationRequestBuilders
                .get("/api/users/staffs") // GET 요청 (스태프 프로필 조회)
                .header(HttpHeaders.AUTHORIZATION, accessTokenWithBearer) // Authorization 헤더로 전달
                .contentType(MediaType.APPLICATION_JSON)
        );
    }

    // 요청시도 - accesstoken이 없는 경우
    private ResultActions getResultActionsForGetStaffProfile() throws Exception {
        return mockMvc.perform(
            RestDocumentationRequestBuilders
                .get("/api/users/staffs") // GET 요청 (스태프 프로필 조회)
                .contentType(MediaType.APPLICATION_JSON)
        );
    }

    // RestDocs 문서화 핸들러
    private RestDocumentationResultHandler getDocumentForGetStaffProfile(String identifier, Boolean isSuccess) throws Exception {
        return document(
            "api/users/getStaffProfile/" + identifier,
            preprocessRequest(prettyPrint(), modifyUris().scheme("https").host("yellowdog.p-e.kr").removePort()),
            preprocessResponse(prettyPrint(), getModifiedHeader()),
            responseFields(isSuccess ? responseFieldDescriptorsForSuccess : responseFieldDescriptorsForFailure), // 응답 필드
            resource(
                ResourceSnippetParameters.builder()
                    .tag("유저-Staff")
                    .summary("스태프(Staff) 정보 조회 API")
                    .description("스태프의 이름, 이메일, 프로필이미지, 보호소명, 보호소 연락처, 보호소 담당자 연락처를 조회하는 API입니다.")
                    .responseFields(
                        isSuccess ? responseFieldDescriptorsForSuccess : responseFieldDescriptorsForFailure
                    )
                    .build()
            )
        );
    }

    // 1. 스태프정보조회 성공 테스트(200)
    @Test
    @Transactional
    void getStaffProfileSuccessTest() throws Exception {
        // 1. Given: 사용자 회원가입
        AuthRequestDto authRequestDto = AuthRequestDto.builder()
            .socialLoginUuid("800a8d7a-442d-4f5b-967e-2ed138f6e789")
            .name("getStaffProfileSuccessTester")
            .email("getStaffProfileSuccessTester@gmail.com")
            .socialLoginProvider("kakao")
            .latitude(BigDecimal.valueOf(12.13123))
            .longitude(BigDecimal.valueOf(14.12312))
            .build();

        HttpHeaders loginResponseHttpHeaders = authService.login(authRequestDto);

        String accessTokenWithBearer = loginResponseHttpHeaders.getFirst(HttpHeaders.AUTHORIZATION);

        // 보호소 저장
        Shelter shelter = Shelter.builder()
            .providerShelterId(1L)
            .name("테스트보호소")
            .cityCode("테스트도시코드")
            .districtCode("테스트지역코드")
            .centerPhoneNumber("02-1234-5678")
            .managerPhoneNumber("010-9876-5432")
            .sido("서울특별시")
            .sigungu("강남구")
            .eupmyeondong("역삼동")
            .roadAddress("테헤란로 123")
            .latitude(new BigDecimal("37.1234567"))
            .longitude(new BigDecimal("127.1234567"))
            .build();

        Shelter savedShelter = shelterRepository.saveAndFlush(shelter);

        // 유저 보호소 지정
        shelterService.registerShelterStaff(authRequestDto.getSocialLoginUuid(), savedShelter.getId());

        // When
        ResultActions resultActions = getResultActionsForGetStaffProfile(accessTokenWithBearer);

        // Then
        resultActions.andExpect(status().isOk()) // 200 OK
            .andExpect(jsonPath("isSuccess").value(true))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.SUCCESS.getCode()))
            .andExpect(jsonPath("data[0].userName").exists())
            .andExpect(jsonPath("data[0].userEmail").exists())
            .andExpect(jsonPath("data[0].userProfileImageUrl").exists())
            .andExpect(jsonPath("data[0].shelterName").exists())
            .andExpect(jsonPath("data[0].shelterCenterPhoneNumber").exists())
            .andExpect(jsonPath("data[0].shelterManagerPhoneNumber").exists());

        // 문서화
        resultActions.andDo(getDocumentForGetStaffProfile("success", true));
    }

    // 2. 스태프정보조회 실패 테스트 - 스태프 정보 없음(404)
    @Test
    @Transactional
    void getStaffProfileFailNullStaffTest() throws Exception {
        // 1. Given: 사용자 회원가입
        AuthRequestDto authRequestDto = AuthRequestDto.builder()
            .socialLoginUuid("456a8d7a-442d-4f5b-967e-2ed138f6e852")
            .name("getStaffProfileFailNullStaffTester")
            .email("getStaffProfileFailNullStaffTester@gmail.com")
            .socialLoginProvider("kakao")
            .latitude(BigDecimal.valueOf(12.13123))
            .longitude(BigDecimal.valueOf(14.12312))
            .build();

        HttpHeaders loginResponseHttpHeaders = authService.login(authRequestDto);

        String accessTokenWithBearer = loginResponseHttpHeaders.getFirst(HttpHeaders.AUTHORIZATION);

        // When
        ResultActions resultActions = getResultActionsForGetStaffProfile(accessTokenWithBearer);

        // Then
        resultActions.andExpect(status().isNotFound()) // 404 NotFound
            .andExpect(jsonPath("isSuccess").value(false))
            .andExpect(jsonPath("status").value(ResponseCodeEnum.STAFF_NOT_FOUND.getStatus().name()))
            .andExpect(jsonPath("code").value(ResponseCodeEnum.STAFF_NOT_FOUND.getCode()));

        // 문서화
        resultActions.andDo(getDocumentForGetStaffProfile("fail-nullstaff", false));
    }

}