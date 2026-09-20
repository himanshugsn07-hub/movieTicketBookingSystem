package com.himanshu.movieTicketBookingSystem.controller;

import com.himanshu.movieTicketBookingSystem.dto.AdminDtos.*;
import com.himanshu.movieTicketBookingSystem.dto.BookingDtos.MovieResponse;
import com.himanshu.movieTicketBookingSystem.service.AdminCatalogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminCatalogController {

    @Autowired
    private AdminCatalogService catalog;

    // Creates a city.
    @PostMapping("/cities")
    @ResponseStatus(HttpStatus.CREATED)
    public CityResponse createCity(@Valid @RequestBody CityRequest r) {
        return CityResponse.from(catalog.createCity(r.name()));
    }

    // Lists all cities.
    @GetMapping("/cities")
    public List<CityResponse> listCities() {
        return catalog.listCities().stream().map(CityResponse::from).toList();
    }

    // Renames a city.
    @PutMapping("/cities/{id}")
    public CityResponse updateCity(@PathVariable int id, @Valid @RequestBody CityRequest r) {
        return CityResponse.from(catalog.updateCity(id, r.name()));
    }

    // Deletes a city.
    @DeleteMapping("/cities/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCity(@PathVariable int id) {
        catalog.deleteCity(id);
    }

    // Creates a theatre.
    @PostMapping("/theatres")
    @ResponseStatus(HttpStatus.CREATED)
    public TheatreResponse createTheatre(@Valid @RequestBody TheatreRequest r) {
        return TheatreResponse.from(catalog.createTheatre(r.name(), r.cityId()));
    }

    // Lists theatres, optionally by city.
    @GetMapping("/theatres")
    public List<TheatreResponse> listTheatres(@RequestParam(required = false) Integer cityId) {
        return catalog.listTheatres(cityId).stream().map(TheatreResponse::from).toList();
    }

    // Updates a theatre.
    @PutMapping("/theatres/{id}")
    public TheatreResponse updateTheatre(@PathVariable int id, @Valid @RequestBody TheatreRequest r) {
        return TheatreResponse.from(catalog.updateTheatre(id, r.name(), r.cityId()));
    }

    // Deletes a theatre.
    @DeleteMapping("/theatres/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTheatre(@PathVariable int id) {
        catalog.deleteTheatre(id);
    }

    // Creates a screen with its seat layout.
    @PostMapping("/screens")
    @ResponseStatus(HttpStatus.CREATED)
    public ScreenResponse createScreen(@Valid @RequestBody ScreenRequest r) {
        return ScreenResponse.from(catalog.createScreen(r.name(), r.theatreId(), r.rows(), r.columns()));
    }

    // Lists screens, optionally by theatre.
    @GetMapping("/screens")
    public List<ScreenResponse> listScreens(@RequestParam(required = false) Integer theatreId) {
        return catalog.listScreens(theatreId).stream().map(ScreenResponse::from).toList();
    }

    // Updates a screen and its seat layout.
    @PutMapping("/screens/{id}")
    public ScreenResponse updateScreen(@PathVariable int id, @Valid @RequestBody ScreenRequest r) {
        return ScreenResponse.from(catalog.updateScreen(id, r.name(), r.theatreId(), r.rows(), r.columns()));
    }

    // Deletes a screen.
    @DeleteMapping("/screens/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteScreen(@PathVariable int id) {
        catalog.deleteScreen(id);
    }

    // Creates a movie.
    @PostMapping("/movies")
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse createMovie(@Valid @RequestBody MovieRequest r) {
        return MovieResponse.from(catalog.createMovie(r.id(), r.title(), r.language(), r.durationMin(), r.genre()));
    }

    // Lists all movies.
    @GetMapping("/movies")
    public List<MovieResponse> listMovies() {
        return catalog.listMovies().stream().map(MovieResponse::from).toList();
    }

    // Updates a movie.
    @PutMapping("/movies/{id}")
    public MovieResponse updateMovie(@PathVariable String id, @Valid @RequestBody MovieRequest r) {
        return MovieResponse.from(catalog.updateMovie(id, r.title(), r.language(), r.durationMin(), r.genre()));
    }

    // Deletes a movie.
    @DeleteMapping("/movies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMovie(@PathVariable String id) {
        catalog.deleteMovie(id);
    }
}
