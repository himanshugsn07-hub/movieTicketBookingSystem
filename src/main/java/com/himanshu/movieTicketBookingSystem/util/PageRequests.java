package com.himanshu.movieTicketBookingSystem.util;

import com.himanshu.movieTicketBookingSystem.constants.Constants;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PageRequests {

    private PageRequests() {
    }

    // Builds a PageRequest, or throws ValidationException if page is negative or size is outside 1..MAX_SIZE.
    public static PageRequest of(int page, int size) {
        return of(page, size, Sort.unsorted());
    }

    // Same as of(page, size) with the given sort order.
    public static PageRequest of(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > Constants.Paging.MAX_SIZE) {
            throw new ValidationException(Constants.Messages.INVALID_PAGING);
        }
        return PageRequest.of(page, size, sort);
    }
}
