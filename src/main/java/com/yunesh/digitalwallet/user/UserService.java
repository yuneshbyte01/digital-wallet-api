package com.yunesh.digitalwallet.user;

import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

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

        user.setPinHash(passwordEncoder.encode(request.pin()));

        userRepository.save(user);
    }
}