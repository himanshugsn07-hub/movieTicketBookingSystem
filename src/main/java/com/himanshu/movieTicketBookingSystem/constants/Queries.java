package com.himanshu.movieTicketBookingSystem.constants;

/**
 * All JPQL queries used by the repositories, grouped by entity.
 * Enum literals are written with their fully qualified names as JPQL requires.
 */
public final class Queries {

    private static final String ENUMS = "com.himanshu.movieTicketBookingSystem.enums.";
    private static final String BOOKING_CREATED = ENUMS + "BookingStatus.CREATED";
    private static final String NOTIFICATION_PENDING = ENUMS + "NotificationStatus.PENDING";
    private static final String NOTIFICATION_CANCELLED = ENUMS + "NotificationStatus.CANCELLED";
    private static final String NOTIFICATION_REMINDER = ENUMS + "NotificationType.REMINDER";

    private Queries() {
    }

    public static final class Booking {

        public static final String EXPIRED_IDS =
                "select b.confirmationId from Booking b "
                        + "where b.bookingStatus = " + BOOKING_CREATED + " and b.expiresAt < :now "
                        + "order by b.expiresAt, b.confirmationId";

        public static final String EXPIRED_IDS_BY_SHOW =
                "select b.confirmationId from Booking b "
                        + "where b.bookingStatus = " + BOOKING_CREATED + " and b.expiresAt < :now and b.show.id = :showId "
                        + "order by b.expiresAt, b.confirmationId";

        public static final String IDS_BY_SHOW_AND_STATUSES =
                "select b.confirmationId from Booking b "
                        + "where b.show.id = :showId and b.bookingStatus in :statuses "
                        + "order by b.confirmationId";

        public static final String BY_ID = "select b from Booking b where b.confirmationId = :confirmationId";

        private Booking() {
        }
    }

    public static final class DiscountCode {

        public static final String BY_CODE = "select d from DiscountCode d where d.code = :code";

        private DiscountCode() {
        }
    }

    public static final class Movie {

        public static final String BY_TITLE_IN_CITY =
                "select distinct s.movie from Show s "
                        + "where lower(s.movie.title) like lower(concat('%', :title, '%')) "
                        + "and s.screen.theatre.city.id = :cityId";

        private Movie() {
        }
    }

    public static final class Notification {

        public static final String BY_ID = "select n from Notification n where n.id = :id";

        public static final String DUE_IDS =
                "select n.id from Notification n "
                        + "where n.status = " + NOTIFICATION_PENDING + " and n.sendAt <= :now "
                        + "order by n.sendAt";

        public static final String CANCEL_PENDING_REMINDERS =
                "update Notification n set n.status = " + NOTIFICATION_CANCELLED + " "
                        + "where n.bookingId = :bookingId and n.type = " + NOTIFICATION_REMINDER
                        + " and n.status = " + NOTIFICATION_PENDING;

        private Notification() {
        }
    }

    public static final class Screen {

        public static final String BY_ID = "select s from Screen s where s.id = :id";

        private Screen() {
        }
    }

    public static final class Seat {

        public static final String BY_SHOW_AND_IDS =
                "select s from Seat s where s.show.id = :showId and s.id in :seatIds order by s.id";

        private Seat() {
        }
    }

    public static final class Show {

        public static final String BY_ID = "select s from Show s where s.id = :id";

        public static final String EXISTS_OVERLAPPING =
                "select case when count(s) > 0 then true else false end from Show s "
                        + "where s.screen.id = :screenId and s.cancelled = false "
                        + "and s.startTime < :end and s.endTime > :start";

        private Show() {
        }
    }
}
