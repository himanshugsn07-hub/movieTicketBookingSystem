package com.himanshu.movieTicketBookingSystem.constants;

import java.math.BigDecimal;
import java.time.Duration;

/** Application-wide constants, grouped by concern. */
public final class Constants {

    private Constants() {
    }

    /** Base URL paths shared by controllers and the security configuration. */
    public static final class Api {
        public static final String ROOT = "/api";
        public static final String AUTH = ROOT + "/auth";
        public static final String REGISTER = AUTH + "/register";
        public static final String ADMIN = ROOT + "/admin";
        public static final String ERROR = "/error";

        private Api() {
        }
    }

    /** Booking hold and reminder timing. */
    public static final class BookingRules {
        public static final Duration HOLD_DURATION = Duration.ofMinutes(5);
        public static final Duration REMINDER_LEAD_TIME = Duration.ofHours(1);

        private BookingRules() {
        }
    }

    /** Price multipliers for the non-regular pricing tiers. */
    public static final class Pricing {
        public static final BigDecimal PREMIUM_MULTIPLIER = new BigDecimal("1.5");
        public static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.5");

        private Pricing() {
        }
    }

    /** Refund policy defaults. */
    public static final class Refunds {
        /** Tiers applied to bookings created before refund policies were configurable. */
        public static final String LEGACY_TIERS = "24:100,0:0";
        public static final String DEFAULT_POLICY_NAME = "Standard";
        public static final int DEFAULT_FULL_REFUND_HOURS = 24;

        private Refunds() {
        }
    }

    /** Limits for a screen's seat layout. */
    public static final class Layout {
        public static final int MAX_ROWS = 26;
        public static final int MAX_COLUMNS = 50;

        private Layout() {
        }
    }

    /** Paging defaults and limits (defaults are strings because annotations take them that way). */
    public static final class Paging {
        public static final String DEFAULT_PAGE = "0";
        public static final String DEFAULT_SIZE = "20";
        public static final int MAX_SIZE = 100;

        private Paging() {
        }
    }

    /** Notification delivery settings. */
    public static final class Notifications {
        public static final String EXECUTOR = "notificationExecutor";
        public static final String THREAD_PREFIX = "notify-";
        public static final int CORE_POOL_SIZE = 2;
        public static final int MAX_POOL_SIZE = 4;
        public static final int QUEUE_CAPACITY = 500;
        public static final int MAX_DELIVERY_ATTEMPTS = 3;
        public static final int DUE_BATCH_SIZE = 50;
        /** How long a worker may hold a claimed notification before another worker may take it over. */
        public static final Duration CLAIM_LEASE = Duration.ofMinutes(2);

        private Notifications() {
        }
    }

    /** Fixed delays, in milliseconds, of the scheduled jobs. */
    public static final class Scheduling {
        public static final long EXPIRY_SWEEP_MS = 30_000L;
        public static final long NOTIFICATION_SWEEP_MS = 15_000L;

        private Scheduling() {
        }
    }

    /** Security settings. */
    public static final class Security {
        public static final String ROLE_PREFIX = "ROLE_";
        public static final int MIN_PASSWORD_LENGTH = 6;
        public static final int MAX_PASSWORD_LENGTH = 100;

        private Security() {
        }
    }

    /** Error messages that more than one class uses. */
    public static final class Messages {
        public static final String NOT_BOOKING_OWNER = "Booking does not belong to the current user";
        public static final String SHOW_STARTED = "Show has already started";
        public static final String BUSY = "Another request is changing the same data. Please retry in a moment.";
        public static final String INVALID_PAGING = "page must be >= 0 and size between 1 and " + Paging.MAX_SIZE;

        private Messages() {
        }
    }
}
