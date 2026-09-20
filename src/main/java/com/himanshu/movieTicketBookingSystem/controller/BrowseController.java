package com.himanshu.movieTicketBookingSystem.controller;

import com.himanshu.movieTicketBookingSystem.constants.Constants;
import com.himanshu.movieTicketBookingSystem.dto.AdminDtos.CityResponse;
import com.himanshu.movieTicketBookingSystem.dto.AdminDtos.TheatreResponse;
import com.himanshu.movieTicketBookingSystem.dto.BookingDtos.MovieResponse;
import com.himanshu.movieTicketBookingSystem.dto.BookingDtos.SeatResponse;
import com.himanshu.movieTicketBookingSystem.dto.BrowseDtos.ShowSummary;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.service.BrowseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(Constants.Api.ROOT)
public class BrowseController {

    private final BrowseService browseService;

    public BrowseController(BrowseService browseService) {
        this.browseService = browseService;
    }

    // Searches movies by title in a city.
    @GetMapping("/movies")
    public List<MovieResponse> searchMovies(@RequestParam String title, @RequestParam int cityId) {
        return browseService.searchMovies(title, cityId).stream().map(MovieResponse::from).toList();
    }

    // Lists all cities.
    @GetMapping("/cities")
    public List<CityResponse> cities() {
        return browseService.listCities().stream().map(CityResponse::from).toList();
    }

    // Lists the theatres of a city.
    @GetMapping("/cities/{cityId}/theatres")
    public List<TheatreResponse> theatres(@PathVariable int cityId) {
        return browseService.listTheatres(cityId).stream().map(TheatreResponse::from).toList();
    }

    // Searches upcoming shows by optional city, theatre, movie and date.
    @GetMapping("/shows")
    public List<ShowSummary> shows(@RequestParam(required = false) Integer cityId,
                                   @RequestParam(required = false) Integer theatreId,
                                   @RequestParam(required = false) String movieId,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   @RequestParam(defaultValue = Constants.Paging.DEFAULT_PAGE) int page,
                                   @RequestParam(defaultValue = Constants.Paging.DEFAULT_SIZE) int size) {
        return browseService.searchShows(cityId, theatreId, movieId, date, page, size).stream()
                .map(s -> ShowSummary.from(s, browseService.countAvailableSeats(s.getId())))
                .toList();
    }

    // Returns one show with its available seat count.
    @GetMapping("/shows/{showId}")
    public ShowSummary show(@PathVariable int showId) {
        Show show = browseService.getShow(showId);
        return ShowSummary.from(show, browseService.countAvailableSeats(showId));
    }

    // Lists the available seats of a show.
    @GetMapping("/shows/{showId}/seats/available")
    public List<SeatResponse> availableSeats(@PathVariable int showId) {
        return browseService.getAvailableSeats(showId).stream().map(SeatResponse::from).toList();
    }

    // Returns the full seat map of a show with each seat's status.
    @GetMapping("/shows/{showId}/seats")
    public List<SeatResponse> seatMap(@PathVariable int showId) {
        return browseService.getSeatMap(showId).stream().map(SeatResponse::from).toList();
    }
}
