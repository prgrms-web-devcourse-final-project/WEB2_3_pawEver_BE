package com.pawever.server.domain.user.dto.response;

import com.pawever.server.domain.user.entity.jpa.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserProfileUpdateResponseDto {
    private boolean isNicknameChanged;
    private User user;
}
