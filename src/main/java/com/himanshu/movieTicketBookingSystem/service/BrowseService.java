package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.City;
import com.himanshu.movieTicketBookingSystem.entity.Movie;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.entity.Theatre;
import com.himanshu.movieTicketBookingSystem.enums.SeatStatus;
import com.himanshu.movieTicketBookingSystem.exception.NotFoundException;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import com.himanshu.movieTicketBookingSystem.repository.CityRepository;
import com.himanshu.movieTicketBookingSystem.repository.MovieRepository;
import com.himanshu.movieTicketBookingSystem.repository.SeatRepository;
import com.himanshu.movieTicketBookingSystem.repository.ShowRepository;
import com.himanshu.movieTicketBookingSystem.repository.TheatreRepository;
import com.himanshu.movieTicketBookingSystem.util.PageRequests;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BrowseService {

    private final CityRepository cityRepo;
    private final TheatreRepository theatreRepo;
    private final ShowRepository showRepo;
    private final SeatRepository seatRepo;
    private final MovieRepository movieRepo;
    private final Clock clock;

    public BrowseService(CityRepository cityRepo, TheatreRepository theatreRepo, ShowRepository showRepo,
                         SeatRepository seatRepo, MovieRepository movieRepo,
            Clock clock) {
        this.cityRepo = cityRepo;
        this.theatreRepo = theatreRepo;
        this.showRepo = showRepo;
        this.seatRepo = seatRepo;
        this.movieRepo = movieRepo;
        this.clock = clock;
    }

    // Searches movies by title that are showing in the given city.
    public List<Movie> searchMovies(String title, int cityId) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Title must not be blank");
        }
        return movieRepo.findByTitleInCity(title.trim(), cityId);
    }

    // Returns the available seats of a show; a cancelled show has none.
    public List<Seat> getAvailableSeats(int showId) {
        if (getShow(showId).isCancelled()) {
            return List.of();
        }
        return seatRepo.findByShowIdAndStatus(showId, SeatStatus.AVAILABLE);
    }

    // Lists all cities ordered by name.
    public List<City> listCities() {
        return cityRepo.findAll(Sort.by("name"));
    }

    // Lists the theatres of a city, or throws NotFoundException if the city does not exist.
    public List<Theatre> listTheatres(int cityId) {
        if (!cityRepo.existsById(cityId)) {
            throw NotFoundException.of("City", cityId);
        }
        return theatreRepo.findByCityId(cityId);
    }

    // Finds upcoming, non-cancelled shows filtered by optional city, theatre, movie and date, earliest first.
    public List<Show> searchShows(Integer cityId, Integer theatreId, String movieId, LocalDate date, int page, int size) {
        LocalDateTime now = LocalDateTime.now(clock);
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
        return showRepo.findAll(spec, PageRequests.of(page, size, Sort.by("startTime"))).getContent();
    }

    // Returns a show by id, or throws NotFoundException.
    public Show getShow(int showId) {
        return showRepo.findById(showId).orElseThrow(() -> NotFoundException.of("Show", showId));
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
