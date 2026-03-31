package com.yunesh.digitalwallet.config;

import lombok.Getter;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Getter
@Configuration
public class EncoderConfig {

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder(12);

    private final BCryptPasswordEncoder pinEncoder =
            new BCryptPasswordEncoder(12);
}