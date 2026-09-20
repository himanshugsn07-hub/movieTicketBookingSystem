package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.City;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.entity.Theatre;
import com.himanshu.movieTicketBookingSystem.enums.SeatStatus;
import com.himanshu.movieTicketBookingSystem.exception.NotFoundException;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import com.himanshu.movieTicketBookingSystem.repository.CityRepository;
import com.himanshu.movieTicketBookingSystem.repository.SeatRepository;
import com.himanshu.movieTicketBookingSystem.repository.ShowRepository;
import com.himanshu.movieTicketBookingSystem.repository.TheatreRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BrowseService {

    @Autowired
    private CityRepository cityRepo;

    @Autowired
    private TheatreRepository theatreRepo;

    @Autowired
    private ShowRepository showRepo;

    @Autowired
    private SeatRepository seatRepo;

    // Lists all cities ordered by name.
    public List<City> listCities() {
        return cityRepo.findAll(Sort.by("name"));
    }

    // Lists the theatres of a city, or throws NotFoundException if the city does not exist.
    public List<Theatre> listTheatres(int cityId) {
        if (!cityRepo.existsById(cityId)) {
            throw new NotFoundException("City " + cityId + " not found");
        }
        return theatreRepo.findByCityId(cityId);
    }

    // Finds upcoming, non-cancelled shows filtered by optional city, theatre, movie and date, earliest first.
    public List<Show> searchShows(Integer cityId, Integer theatreId, String movieId, LocalDate date, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ValidationException("page must be >= 0 and size between 1 and 100");
        }
        LocalDateTime now = LocalDateTime.now();
        Specification<Show> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("cancelled")));
            p.add(cb.greaterThan(root.<LocalDateTime>get("startTime"), now));
            if (cityId != null) {
                p.add(cb.equal(root.get("screen").get("theatre").get("city").get("id"), cityId));
            }
            if (theatreId != null) {
                p.add(cb.equal(root.get("screen").get("theatre").get("id"), theatreId));
            }
            if (movieId != null && !movieId.isBlank()) {
                p.add(cb.equal(root.get("movie").get("id"), movieId));
            }
            if (date != null) {
                p.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("startTime"), date.atStartOfDay()));
                p.add(cb.lessThan(root.<LocalDateTime>get("startTime"), date.plusDays(1).atStartOfDay()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return showRepo.findAll(spec, PageRequest.of(page, size, Sort.by("startTime"))).getContent();
    }

    // Returns a show by id, or throws NotFoundException.
    public Show getShow(int showId) {
        return showRepo.findById(showId).orElseThrow(() -> new NotFoundException("Show " + showId + " not found"));
    }

    // Counts the AVAILABLE seats of a show.
    public long countAvailableSeats(int showId) {
        return seatRepo.countByShowIdAndStatus(showId, SeatStatus.AVAILABLE);
    }

    // Returns the full seat map of a show with each seat's status.
    public List<Seat> getSeatMap(int showId) {
        getShow(showId);
        return seatRepo.findByShowIdOrderById(showId);
    }
}
