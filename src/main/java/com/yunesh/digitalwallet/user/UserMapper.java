package com.yunesh.digitalwallet.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", expression = "java(user.getRole().name())")
    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    @Mapping(target = "kycStatus", expression = "java(user.getKycStatus().name())")
    UserProfileResponse toProfileResponse(User user);
}