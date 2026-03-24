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
        // filled in Phase 4 when transfer engine is built
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