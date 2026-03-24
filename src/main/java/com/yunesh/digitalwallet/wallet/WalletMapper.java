package com.yunesh.digitalwallet.wallet;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "status", expression = "java(wallet.getStatus().name())")
    WalletResponse toWalletResponse(Wallet wallet);
}