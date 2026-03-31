package com.yunesh.digitalwallet.kyc;

import com.yunesh.digitalwallet.user.KycStatus;
import com.yunesh.digitalwallet.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface KycMapper {

    @Mapping(target = "userId",
            expression = "java(detail.getUser().getId())")
    @Mapping(target = "maritalStatus",
            expression = "java(detail.getMaritalStatus() != null ? detail.getMaritalStatus().name() : null)")
    @Mapping(target = "documentType",
            expression = "java(detail.getDocumentType() != null ? detail.getDocumentType().name() : null)")
    @Mapping(target = "kycStatus",
            expression = "java(detail.getUser().getKycStatus().name())")
    KycDetailResponse toResponse(KycDetail detail);
}