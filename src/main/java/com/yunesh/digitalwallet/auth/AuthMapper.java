package com.yunesh.digitalwallet.auth;

import com.yunesh.digitalwallet.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "gender",
            expression = "java(user.getGender() != null ? user.getGender().name() : null)")
    @Mapping(target = "role",
            expression = "java(user.getRole().name())")
    @Mapping(target = "kycStatus",
            expression = "java(user.getKycStatus().name())")
    RegisterResponse toRegisterResponse(User user);

    default LoginResponse toLoginResponse(User user,
                                          String accessToken,
                                          String refreshToken) {
        return new LoginResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}