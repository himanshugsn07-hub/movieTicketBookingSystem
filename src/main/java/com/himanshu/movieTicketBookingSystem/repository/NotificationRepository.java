package com.himanshu.movieTicketBookingSystem.repository;

import com.himanshu.movieTicketBookingSystem.entity.Notification;
import com.himanshu.movieTicketBookingSystem.enums.NotificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Finds a notification under a write lock so only one worker delivers it.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notification n where n.id = :id")
    Optional<Notification> findByIdForUpdate(@Param("id") long id);

    // Finds ids of PENDING notifications that are due, oldest first.
    @Query("select n.id from Notification n where n.status = com.himanshu.movieTicketBookingSystem.enums.NotificationStatus.PENDING and n.sendAt <= :now order by n.sendAt")
    List<Long> findDueIds(@Param("now") LocalDateTime now, Pageable pageable);

    // Withdraws the booking's reminders that have not been sent yet.
    @Modifying
    @Query("update Notification n set n.status = com.himanshu.movieTicketBookingSystem.enums.NotificationStatus.CANCELLED where n.bookingId = :bookingId and n.type = com.himanshu.movieTicketBookingSystem.enums.NotificationType.REMINDER and n.status = com.himanshu.movieTicketBookingSystem.enums.NotificationStatus.PENDING")
    int cancelPendingReminders(@Param("bookingId") String bookingId);

    // Finds the user's notifications except withdrawn ones, newest first.
    List<Notification> findByUserIdAndStatusNotOrderByCreatedAtDesc(int userId, NotificationStatus status, Pageable pageable);
}
