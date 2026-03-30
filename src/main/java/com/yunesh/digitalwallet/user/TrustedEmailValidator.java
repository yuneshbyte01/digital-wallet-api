package com.yunesh.digitalwallet.user;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class TrustedEmailValidator
        implements ConstraintValidator<TrustedEmail, String> {

    private static final Set<String> TRUSTED_DOMAINS = Set.of(
            "gmail.com",
            "outlook.com",
            "hotmail.com",
            "yahoo.com",
            "example.com",
            "icloud.com"
    );

    @Override
    public boolean isValid(String email,
                           ConstraintValidatorContext context) {
        if (email == null || !email.contains("@")) return false;
        String domain = email.substring(email.lastIndexOf('@') + 1)
                .toLowerCase();
        return TRUSTED_DOMAINS.contains(domain);
    }
}