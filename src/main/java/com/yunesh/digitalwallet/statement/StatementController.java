package com.yunesh.digitalwallet.statement;

import com.yunesh.digitalwallet.common.ApiResponse;
import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import com.yunesh.digitalwallet.user.User;
import com.yunesh.digitalwallet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/statements")
@RequiredArgsConstructor
public class StatementController {

    private final StatementService statementService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ApiResponse<List<StatementResponse>> getMyStatements(
            @AuthenticationPrincipal String email) {
        User user = getUser(email);
        return ApiResponse.success(
                statementService.getUserStatements(user.getId()));
    }

    @PostMapping("/generate")
    public ApiResponse<StatementResponse> generateStatement(
            @AuthenticationPrincipal String email,
            @RequestParam int month,
            @RequestParam int year) {
        User user = getUser(email);
        return ApiResponse.success(
                statementService.generate(user.getId(), month, year),
                "Statement generated", 200);
    }

    @GetMapping("/{year}/{month}")
    public ApiResponse<StatementResponse> getStatement(
            @AuthenticationPrincipal String email,
            @PathVariable int year,
            @PathVariable int month) {
        User user = getUser(email);
        return ApiResponse.success(
                statementService.getStatement(
                        user.getId(), user.getId(), month, year));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
    }
}