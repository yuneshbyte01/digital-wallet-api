package com.yunesh.digitalwallet.user;

import com.yunesh.digitalwallet.common.AppConstants;
import com.yunesh.digitalwallet.config.EncoderConfig;
import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EncoderConfig encoderConfig;

    @Transactional(readOnly = true)
    public UserProfileResponse findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
        return userMapper.toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(String email,
                                             UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));

        user.setFullName(request.fullName());
        user.setPhone(request.phone());

        return userMapper.toProfileResponse(userRepository.save(user));
    }

    @Transactional
    public void submitKyc(String email, KycRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));

        user.setKycDocType(request.docType());
        user.setKycDocNumber(request.docNumber());
        user.setKycStatus(KycStatus.PENDING);

        userRepository.save(user);
    }

    @Transactional
    public void setPin(String email, SetPinRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));

        user.setPinHash(encoderConfig.getPinEncoder().encode(request.pin()));

        userRepository.save(user);
    }

    @Transactional
    public void toggleLock(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userId));

        if (user.getStatus() == AccountStatus.LOCKED) {
            user.setStatus(AccountStatus.ACTIVE);
            user.setPinAttempts(0);
        } else {
            user.setStatus(AccountStatus.LOCKED);
        }

        userRepository.save(user);
    }

    @Transactional
    public void updateKycStatus(UUID userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userId));

        user.setKycStatus(KycStatus.valueOf(status.toUpperCase()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUsers(int page, int size) {
        if (size > AppConstants.Pagination.MAX_PAGE_SIZE) {
            size = AppConstants.Pagination.MAX_PAGE_SIZE;
        }
        return userRepository.findAll(
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(userMapper::toProfileResponse);
    }
}