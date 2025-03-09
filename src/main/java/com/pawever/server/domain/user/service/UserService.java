package com.pawever.server.domain.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawever.server.common.exception.CustomException;
import com.pawever.server.common.response.ApiResponse;
import com.pawever.server.common.response.ResponseCodeEnum;
import com.pawever.server.domain.carehub.entity.Shelter;
import com.pawever.server.domain.post.service.ImageService;
import com.pawever.server.domain.user.dto.request.AuthRequestDto;
import com.pawever.server.domain.user.dto.request.UserProfileUpdateRequestDto;
import com.pawever.server.domain.user.dto.response.StaffProfileResponseDto;
import com.pawever.server.domain.user.dto.response.UserProfileResponseDto;
import com.pawever.server.domain.user.dto.response.UserProfileUpdateResponseDto;
import com.pawever.server.domain.user.dto.response.UserResponseDto;
import com.pawever.server.domain.user.entity.jpa.User;
import com.pawever.server.domain.user.enums.Role;
import com.pawever.server.domain.user.jwt.JwtUtil;
import com.pawever.server.domain.user.repository.jpa.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenService accessTokenService;
    private final JwtUtil jwtUtil;
    private final ImageService imageService;
    private final UserImageService userImageService;
    private final ObjectMapper objectMapper;


    public UserResponseDto getUserInfoByUuid(String socialLoginUuid){

        Optional<User> foundUser = userRepository.findBySocialLoginUuid(socialLoginUuid);
        return foundUser.map(user -> UserResponseDto.builder()
            .userId(user.getUserId())
            .socialLoginUuid(user.getSocialLoginUuid())
            .name(user.getName())
            .role(user.getRole())
            .isDeleted(user.getIsDeleted())
            .build()).orElse(null);
    }

    public User createNewUser(AuthRequestDto authRequestDto) {
        return User.builder()
            .name(authRequestDto.getName())
            .email(authRequestDto.getEmail())
            .profileImageUrl(authRequestDto.getProfileImageUrl())
            .socialLoginUuid(authRequestDto.getSocialLoginUuid())
            .socialLoginProvider(authRequestDto.getSocialLoginProvider())
            .latitude(authRequestDto.getLatitude())
            .longitude(authRequestDto.getLongitude())
            .role(Role.ROLE_USER)
            .isDeleted(false)
            .build();
    }

    @Transactional
    public UserResponseDto saveNewUser(User user) {

        try {
            User savedUser = userRepository.save(user);

            log.info("회원가입 완료 : 회원ID - {}", user.getUserId());

            return UserResponseDto.builder()
                .userId(savedUser.getUserId())
                .socialLoginUuid(savedUser.getSocialLoginUuid())
                .name(savedUser.getName())
                .role(savedUser.getRole())
                .build();
        } catch (DataIntegrityViolationException e) {
            log.error("회원가입실패 : 회원ID - {}, 이유 - {}", user.getUserId(), e.getMessage());
            throw new CustomException(ResponseCodeEnum.MISSING_REQUIRED_FIELDS);
        }
    }

    @Transactional
    public void softDeleteUserByUuid(HttpServletRequest request) {
        // 1. request로부터 refresh토큰 추출(logout 단계에서 유효성 검증했으므로 유효성 체크 생략)
        String refreshToken = refreshTokenService.getRequestRefreshToken(request);

        // 2. refresh 토큰에서 유저 Uuid 추출
        String userUuid = jwtUtil.getSocialLoginUuid(refreshToken);

        // 3. 유저가 존재하는지 확인 후 Soft Delete 실행
        User user = userRepository.findBySocialLoginUuid(userUuid)
            .orElseThrow(()->new CustomException(ResponseCodeEnum.USER_NOT_FOUND));

        // 4. s3에서 프로필 이미지 제거
        userImageService.deleteUserOldProfileImage(user);

        // 5. Soft Delete 실행
        userRepository.softDeleteByUuid(userUuid);

        // 6. staff인 경우, shelter 테이블에서 본인 user_id가 조회되는 경우 전부 null로 변경
        if(user.getRole() == Role.ROLE_STAFF){
            List<Shelter> staffShelterList = user.getShelters();
            if(!staffShelterList.isEmpty()){
                for(Shelter shelter : staffShelterList){
                    log.info("[회원탈퇴] shelter 테이블내 담당자 user_id 제거 시작");
                    shelter.clearUserReference();
                    log.info("[회원탈퇴] shelter 테이블내 담당자 user_id 제거 완료");
                }
            }
        }
    }

    @Transactional
    public void hardDeleteUserByUuid(UserResponseDto userResponseDto) {
        // Hard Delete 실행
        userRepository.hardDeleteByUuid(userResponseDto.getSocialLoginUuid());
    }


    public void logout(HttpServletRequest request, HttpServletResponse response){

        // 1. Request로부터 Refresh Token 가져와서 유효성 검증
        String refreshToken = refreshTokenService.getValidRefreshToken(request);

        // 2. 유효한 Refresh Token을 제거 -> 토큰 갱신 방지
        refreshTokenService.removeRefreshToken(refreshToken);

        // 3. 쿠키 초기화
        Cookie cookie = new Cookie("refresh", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);  // HttpOnly 플래그 추가 (JavaScript에서 접근 불가)
        cookie.setSecure(false);   // (HTTP 환경에서는 `Secure` 설정을 false로 해주세요)

        response.addCookie(cookie);
    }

    public void createLogoutResponse(HttpServletResponse response){
        try {
            response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.success(ResponseCodeEnum.SUCCESS)));
        } catch (IOException e) {
            // 응답생성실패시 500 INTERNAL SERVER ERROR 반환
            throw new CustomException(ResponseCodeEnum.API_RESPONSE_WRITE_FAILED);
        }
        response.setStatus(HttpServletResponse.SC_OK);  // 200 응답코드 전송
    }

    public UserProfileResponseDto getUserProfiles(HttpServletRequest request) {
        String userSocialLoginUuid = accessTokenService.getRequestSocialLoginUuid(request);

        // 유저 정보 조회
        Optional<User> optionalUser = userRepository.findBySocialLoginUuid(userSocialLoginUuid);

        return optionalUser.map(user -> UserProfileResponseDto.builder()
                .name(user.getName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .build())
            .orElseThrow(() -> new CustomException(ResponseCodeEnum.USER_NOT_FOUND));
    }

    @Transactional
    public UserProfileUpdateResponseDto updateUserProfile(UserProfileUpdateRequestDto userProfileUpdateRequestDto,
        MultipartFile profileImageFile,
        HttpServletRequest request){

        if(userProfileUpdateRequestDto!=null ){
            log.info("닉네임: " + userProfileUpdateRequestDto.getName());
            log.info("자기소개: " + userProfileUpdateRequestDto.getIntroduction());
        }else{
            log.info("userProfileUpdateRequestDto == null");
        }

        if(profileImageFile!=null){
            log.info("프로필 이미지: " + profileImageFile.getOriginalFilename());
        }else{
            log.info("profileImageFile == null");
        }


        // 1. request로부터 Uuid 추출
        String socialLoginUuid = accessTokenService.getRequestSocialLoginUuid(request);

        // 2. User 조회
        User user = userRepository.findBySocialLoginUuid(socialLoginUuid)
            .orElseThrow(()->new CustomException(ResponseCodeEnum.USER_NOT_FOUND));

        boolean isNicknameChanged = false;
        if(userProfileUpdateRequestDto!=null){
            isNicknameChanged = userProfileUpdateRequestDto.getName()!=null
                && !userProfileUpdateRequestDto.getName().equals(user.getName());
        }

        // 3. 닉네임 및 자기소개 DB 업데이트
        if(userProfileUpdateRequestDto != null){
            user.updateUserProfile(userProfileUpdateRequestDto.getName(), userProfileUpdateRequestDto.getIntroduction());
        }

        // 4. 프로필 이미지 DB 업데이트
        if(profileImageFile != null && !profileImageFile.isEmpty()){
            String newProfileImageUrl = imageService.uploadImageToS3(profileImageFile);

            // s3에서 기존 이미지 삭제
            userImageService.deleteUserOldProfileImage(user);

            // 새로운 이미지로 업로드
            user.updateProfileImageUrl(newProfileImageUrl);
        }

        return UserProfileUpdateResponseDto.builder()
            .isNicknameChanged(isNicknameChanged)
            .user(user)
            .build();
    }

    public User findUserByUuid(String socialLoginUuid){
        return userRepository.findBySocialLoginUuid(socialLoginUuid).orElseThrow(()-> new CustomException(ResponseCodeEnum.USER_NOT_FOUND));
    }

    @Transactional
    public void updateLocationIfChanged(Long userId, AuthRequestDto authRequestDto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ResponseCodeEnum.USER_NOT_FOUND));
        // 입력된 위도, 경도 값이 존재하고, 기존 DB 값과 다를 경우 업데이트
        if (authRequestDto.getLatitude() != null && authRequestDto.getLongitude() != null) {
            if (!authRequestDto.getLatitude().equals(user.getLatitude()) ||
                !authRequestDto.getLongitude().equals(user.getLongitude())) {

                user.setLatitude(authRequestDto.getLatitude());
                user.setLongitude(authRequestDto.getLongitude());
                userRepository.save(user);
            }
        }
    }

    public List<StaffProfileResponseDto> getStaffProfiles(HttpServletRequest request) {
        String staffAccessToken = accessTokenService.getRequestAccessToken(request);
        Long staffUserId = jwtUtil.getUserId(staffAccessToken);

        List<StaffProfileResponseDto> staffProfiles = userRepository.findStaffProfileByUserId(staffUserId);

        if(staffProfiles.isEmpty()){
            throw new CustomException(ResponseCodeEnum.STAFF_NOT_FOUND);        // 404 NOTFOUND 반환
        }

        return staffProfiles;
    }


    @Transactional
    public void updateUserRoles(String inputRole, HttpServletRequest request){
        String userUuid = accessTokenService.getRequestSocialLoginUuid(request);
        // 1. 유저 조회
        User user = userRepository.findBySocialLoginUuid(userUuid)
            .orElseThrow(()-> new CustomException(ResponseCodeEnum.USER_NOT_FOUND));

        // 2. ROLE UPDATE
        List<Role> roleList = Role.getRoles();
        Role targetRole = roleList.stream()
            .filter(role -> role.name().replace("ROLE_", "").equalsIgnoreCase(inputRole))
            .findFirst()
            .orElseThrow(()-> new CustomException(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT));

        user.updateUserRole(targetRole);

        // 3. 변경된 user 정보 저장
        userRepository.save(user);
    }

}
