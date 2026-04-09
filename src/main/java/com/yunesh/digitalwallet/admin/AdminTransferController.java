package com.yunesh.digitalwallet.admin;

import com.yunesh.digitalwallet.common.ApiResponse;
import com.yunesh.digitalwallet.common.AppConstants;
import com.yunesh.digitalwallet.transfer.TransferResponse;
import com.yunesh.digitalwallet.transfer.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/transfers")
@RequiredArgsConstructor
public class AdminTransferController {

    private final TransferService transferService;

    @GetMapping
    public ApiResponse<Page<TransferResponse>> getAllTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (size > AppConstants.Pagination.MAX_PAGE_SIZE) {
            size = AppConstants.Pagination.MAX_PAGE_SIZE;
        }

        return ApiResponse.success(
                transferService.getAllTransfers(page, size));
    }

    @PostMapping("/{transferId}/reverse")
    public ApiResponse<TransferResponse> reverse(
            @PathVariable UUID transferId) {
        return ApiResponse.success(
                transferService.reverseTransfer(transferId),
                "Transfer reversed successfully", 200);
    }
}