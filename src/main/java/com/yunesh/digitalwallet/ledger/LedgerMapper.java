package com.yunesh.digitalwallet.ledger;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LedgerMapper {

    @Mapping(target = "debitWalletId",
            expression = "java(entry.getDebitWallet().getId())")
    @Mapping(target = "creditWalletId",
            expression = "java(entry.getCreditWallet().getId())")
    @Mapping(target = "entryType",
            expression = "java(entry.getEntryType().name())")
    LedgerEntryResponse toResponse(LedgerEntry entry);
}