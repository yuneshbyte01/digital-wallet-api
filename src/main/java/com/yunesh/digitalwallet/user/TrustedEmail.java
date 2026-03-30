package com.yunesh.digitalwallet.user;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Email;

import java.lang.annotation.*;

@Email
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TrustedEmailValidator.class)
@Documented
public @interface TrustedEmail {
    String message() default
            "Email must be from a trusted provider (Gmail, Outlook, iCloud, ProtonMail, Yahoo)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}