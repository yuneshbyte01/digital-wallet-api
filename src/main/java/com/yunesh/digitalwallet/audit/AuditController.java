package com.yunesh.digitalwallet.audit;

import com.yunesh.digitalwallet.common.ApiResponse;
import com.yunesh.digitalwallet.common.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/compliance/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditRepository auditRepository;

    @GetMapping
    public ApiResponse<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (size > AppConstants.Pagination.MAX_PAGE_SIZE) {
            size = AppConstants.Pagination.MAX_PAGE_SIZE;
        }

        Page<AuditLogResponse> logs = auditRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(AuditLogResponse::from);

        return ApiResponse.success(logs);
    }
}
