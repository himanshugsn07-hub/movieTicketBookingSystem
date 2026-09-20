package com.himanshu.movieTicketBookingSystem.controller;

import com.himanshu.movieTicketBookingSystem.dto.AdminDtos.CityResponse;
import com.himanshu.movieTicketBookingSystem.dto.AdminDtos.TheatreResponse;
import com.himanshu.movieTicketBookingSystem.dto.BookingDtos.SeatResponse;
import com.himanshu.movieTicketBookingSystem.dto.BrowseDtos.ShowSummary;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.service.BrowseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BrowseController {

    @Autowired
    private BrowseService browseService;

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
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
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

    // Returns the full seat map of a show with each seat's status.
    @GetMapping("/shows/{showId}/seats")
    public List<SeatResponse> seatMap(@PathVariable int showId) {
        return browseService.getSeatMap(showId).stream().map(SeatResponse::from).toList();
    }
}
