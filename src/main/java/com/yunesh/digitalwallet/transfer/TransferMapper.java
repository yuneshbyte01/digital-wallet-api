package com.yunesh.digitalwallet.transfer;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransferMapper {

    @Mapping(target = "senderWalletId",
            expression = "java(transfer.getSenderWallet().getId())")
    @Mapping(target = "receiverWalletId",
            expression = "java(transfer.getReceiverWallet().getId())")
    @Mapping(target = "status",
            expression = "java(transfer.getStatus().name())")
    TransferResponse toResponse(Transfer transfer);
}