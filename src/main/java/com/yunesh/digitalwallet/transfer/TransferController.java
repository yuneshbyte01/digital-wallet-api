package com.yunesh.digitalwallet.transfer;

import com.yunesh.digitalwallet.common.ApiResponse;
import com.yunesh.digitalwallet.common.AppConstants;
import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/lookup")
    public ApiResponse<ReceiverLookupResponse> lookup(
            @Valid @RequestBody ReceiverLookupRequest request) {
        return ApiResponse.success(
                transferService.lookupReceiver(request.identifier()),
                "Receiver found", 200);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransferResponse> transfer(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody TransferRequest request) {
        return ApiResponse.success(
                transferService.executeTransfer(email, request),
                "Transfer completed successfully", 201);
    }

    @GetMapping("/me")
    public ApiResponse<Page<TransferResponse>> getMyTransfers(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (size > AppConstants.Pagination.MAX_PAGE_SIZE) {
            size = AppConstants.Pagination.MAX_PAGE_SIZE;
        }

        return ApiResponse.success(
                transferService.getMyTransfers(email, page, size));
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