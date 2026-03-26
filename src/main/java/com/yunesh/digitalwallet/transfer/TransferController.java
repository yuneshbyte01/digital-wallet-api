package com.yunesh.digitalwallet.transfer;

import com.yunesh.digitalwallet.common.ApiResponse;
import com.yunesh.digitalwallet.common.AppConstants;
import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final TransferRepository transferRepository;
    private final TransferMapper transferMapper;

    @PostMapping
    public ApiResponse<TransferResponse> transfer(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody TransferRequest request) {
        return ApiResponse.success(
                transferService.executeTransfer(email, request),
                "Transfer successful", 200);
    }

    @GetMapping
    public ApiResponse<Page<TransferResponse>> getTransfers(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (size > AppConstants.Pagination.MAX_PAGE_SIZE) {
            size = AppConstants.Pagination.MAX_PAGE_SIZE;
        }

        // get sender wallet — reuse existing service method via repository
        Page<TransferResponse> transfers = transferRepository
                .findBySenderWalletId(
                        transferService.getSenderWalletId(email),
                        PageRequest.of(page, size,
                                Sort.by("createdAt").descending()))
                .map(transferMapper::toResponse);

        return ApiResponse.success(transfers);
    }

    @GetMapping("/{id}")
    public ApiResponse<TransferResponse> getTransfer(
            @PathVariable UUID id) {
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transfer not found: " + id));
        return ApiResponse.success(transferMapper.toResponse(transfer));
    }
}