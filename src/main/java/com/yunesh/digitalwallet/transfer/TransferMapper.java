package com.yunesh.digitalwallet.transfer;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransferMapper {

    @Mapping(target = "transferId", source = "id")
    @Mapping(target = "status",
            expression = "java(transfer.getStatus().name())")
    @Mapping(target = "processedAt", source = "completedAt")
    @Mapping(target = "currency",
            expression = "java(\"NPR\")")
    @Mapping(target = "purpose",
            expression = "java(transfer.getPurpose() != null ? transfer.getPurpose().name() : null)")
    @Mapping(target = "sender.fullName",
            expression = "java(transfer.getSenderWallet().getUser().getFullName())")
    @Mapping(target = "sender.phone",
            expression = "java(transfer.getSenderWallet().getUser().getPhone())")
    @Mapping(target = "sender.email",
            expression = "java(transfer.getSenderWallet().getUser().getEmail())")
    @Mapping(target = "receiver.fullName",
            expression = "java(transfer.getReceiverWallet().getUser().getFullName())")
    @Mapping(target = "receiver.phone",
            expression = "java(transfer.getReceiverWallet().getUser().getPhone())")
    @Mapping(target = "receiver.email",
            expression = "java(transfer.getReceiverWallet().getUser().getEmail())")
    TransferResponse toResponse(Transfer transfer);
}