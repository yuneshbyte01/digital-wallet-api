package com.yunesh.digitalwallet.common;

import java.math.BigDecimal;

public class AppConstants {

    private AppConstants() {}

    public static class Security {
        private Security() {}

        public static final int    MAX_PIN_ATTEMPTS = 3;
        public static final String BEARER_PREFIX    = "Bearer ";
        public static final String TOKEN_HEADER     = "Authorization";
    }

    public static class Transfer {
        private Transfer() {}

        public static final BigDecimal DAILY_LIMIT_NPR  = new BigDecimal("100000.00");
        public static final BigDecimal SINGLE_LIMIT_NPR = new BigDecimal("25000.00");
        public static final BigDecimal MIN_AMOUNT       = new BigDecimal("10.00");
    }

    public static class Pagination {
        private Pagination() {}

        public static final int DEFAULT_PAGE_SIZE = 20;
        public static final int MAX_PAGE_SIZE     = 100;
    }

    public static class RateLimit {
        private RateLimit() {}
        // filled in Phase 4 when Bucket4j is added
    }
}