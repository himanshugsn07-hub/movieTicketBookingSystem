package com.himanshu.movieTicketBookingSystem.controller;

import com.himanshu.movieTicketBookingSystem.dto.NotificationDtos.NotificationResponse;
import com.himanshu.movieTicketBookingSystem.security.AppUserDetails;
import com.himanshu.movieTicketBookingSystem.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // Lists the logged-in user's notifications, newest first.
    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal AppUserDetails user,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return notificationService.list(user.getId(), page, size).stream().map(NotificationResponse::from).toList();
    }
}
