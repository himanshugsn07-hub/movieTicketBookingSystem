package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.Movie;
import com.himanshu.movieTicketBookingSystem.entity.Screen;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.enums.PricingTier;
import com.himanshu.movieTicketBookingSystem.exception.ConflictException;
import com.himanshu.movieTicketBookingSystem.exception.InvalidStateException;
import com.himanshu.movieTicketBookingSystem.exception.NotFoundException;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import com.himanshu.movieTicketBookingSystem.repository.MovieRepository;
import com.himanshu.movieTicketBookingSystem.repository.RefundPolicyConfigRepository;
import com.himanshu.movieTicketBookingSystem.repository.ScreenRepository;
import com.himanshu.movieTicketBookingSystem.repository.ShowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AdminShowService {

    @Autowired
    private ShowRepository showRepo;

    @Autowired
    private ScreenRepository screenRepo;

    @Autowired
    private MovieRepository movieRepo;

    @Autowired
    private RefundPolicyConfigRepository refundPolicyRepo;

    // Schedules a show on a screen (locked to serialize scheduling) and generates its seats from the screen layout.
    public Show createShow(int screenId, String movieId, BigDecimal basePrice, PricingTier pricingTier,
                           LocalDateTime startTime) {
        Screen screen = screenRepo.findByIdForUpdate(screenId)
                .orElseThrow(() -> new NotFoundException("Screen " + screenId + " not found"));
        Movie movie = movieRepo.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie " + movieId + " not found"));
        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new ValidationException("Show must start in the future");
        }
        LocalDateTime endTime = startTime.plusMinutes(movie.getDurationMin());
        if (showRepo.existsOverlapping(screenId, startTime, endTime)) {
            throw new ConflictException("Screen already has a show overlapping " + startTime + " - " + endTime);
        }
        Show show = new Show(screen, movie, basePrice, pricingTier, startTime, endTime);
        show.generateSeats(screen.getRows(), screen.getColumns());
        return showRepo.save(show);
    }

    // Assigns a refund policy to a show that has not started, or clears it (null) to use the default policy.
    public Show assignRefundPolicy(int showId, Integer refundPolicyId) {
        Show show = showRepo.findById(showId)
                .orElseThrow(() -> new NotFoundException("Show " + showId + " not found"));
        if (show.isCancelled() || show.hasStarted()) {
            throw new InvalidStateException("Cannot change the refund policy of a cancelled or started show");
        }
        show.assignRefundPolicy(refundPolicyId == null ? null : refundPolicyRepo.findById(refundPolicyId)
                .orElseThrow(() -> new NotFoundException("Refund policy " + refundPolicyId + " not found")));
        return show;
    }

    // Lists shows, optionally filtered by screen.
    @Transactional(readOnly = true)
    public List<Show> listShows(Integer screenId) {
        return screenId == null ? showRepo.findAll() : showRepo.findByScreenId(screenId);
    }

    // Changes a show's base price and pricing tier; not allowed once it is cancelled or started.
    public Show updatePricing(int showId, BigDecimal basePrice, PricingTier pricingTier) {
        Show show = showRepo.findById(showId)
                .orElseThrow(() -> new NotFoundException("Show " + showId + " not found"));
        if (show.isCancelled() || show.hasStarted()) {
            throw new InvalidStateException("Cannot reprice a cancelled or started show");
        }
        show.reprice(basePrice, pricingTier);
        return show;
    }
}
