package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.City;
import com.himanshu.movieTicketBookingSystem.entity.Movie;
import com.himanshu.movieTicketBookingSystem.entity.Screen;
import com.himanshu.movieTicketBookingSystem.entity.Theatre;
import com.himanshu.movieTicketBookingSystem.exception.ConflictException;
import com.himanshu.movieTicketBookingSystem.exception.NotFoundException;
import com.himanshu.movieTicketBookingSystem.repository.CityRepository;
import com.himanshu.movieTicketBookingSystem.repository.MovieRepository;
import com.himanshu.movieTicketBookingSystem.repository.ScreenRepository;
import com.himanshu.movieTicketBookingSystem.repository.TheatreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminCatalogService {

    private final CityRepository cityRepo;
    private final TheatreRepository theatreRepo;
    private final ScreenRepository screenRepo;
    private final MovieRepository movieRepo;

    public AdminCatalogService(CityRepository cityRepo, TheatreRepository theatreRepo, ScreenRepository screenRepo, MovieRepository movieRepo) {
        this.cityRepo = cityRepo;
        this.theatreRepo = theatreRepo;
        this.screenRepo = screenRepo;
        this.movieRepo = movieRepo;
    }

    // Creates a city.
    public City createCity(String name) {
        return cityRepo.save(new City(name.trim()));
    }

    // Lists all cities.
    @Transactional(readOnly = true)
    public List<City> listCities() {
        return cityRepo.findAll();
    }

    // Renames a city.
    public City updateCity(int id, String name) {
        City city = findCity(id);
        city.rename(name.trim());
        return city;
    }

    // Deletes a city; fails with a conflict if it still has theatres.
    public void deleteCity(int id) {
        cityRepo.delete(findCity(id));
        cityRepo.flush();
    }

    // Creates a theatre in an existing city.
    public Theatre createTheatre(String name, int cityId) {
        return theatreRepo.save(new Theatre(name.trim(), findCity(cityId)));
    }

    // Lists theatres, optionally filtered by city.
    @Transactional(readOnly = true)
    public List<Theatre> listTheatres(Integer cityId) {
        return cityId == null ? theatreRepo.findAll() : theatreRepo.findByCityId(cityId);
    }

    // Updates a theatre's name and city.
    public Theatre updateTheatre(int id, String name, int cityId) {
        Theatre theatre = findTheatre(id);
        theatre.update(name.trim(), findCity(cityId));
        return theatre;
    }

    // Deletes a theatre; fails with a conflict if it still has screens.
    public void deleteTheatre(int id) {
        theatreRepo.delete(findTheatre(id));
        theatreRepo.flush();
    }

    // Creates a screen with a rows x columns seat layout in an existing theatre.
    public Screen createScreen(String name, int theatreId, int rows, int columns) {
        return screenRepo.save(new Screen(name.trim(), findTheatre(theatreId), rows, columns));
    }

    // Lists screens, optionally filtered by theatre.
    @Transactional(readOnly = true)
    public List<Screen> listScreens(Integer theatreId) {
        return theatreId == null ? screenRepo.findAll() : screenRepo.findByTheatreId(theatreId);
    }

    // Updates a screen and its seat layout; existing shows keep their seats.
    public Screen updateScreen(int id, String name, int theatreId, int rows, int columns) {
        Screen screen = findScreen(id);
        screen.update(name.trim(), findTheatre(theatreId), rows, columns);
        return screen;
    }

    // Deletes a screen; fails with a conflict if it still has shows.
    public void deleteScreen(int id) {
        screenRepo.delete(findScreen(id));
        screenRepo.flush();
    }

    // Creates a movie, generating an id when none is given; fails with a conflict if the id exists.
    public Movie createMovie(String id, String title, String language, int durationMin, String genre) {
        String movieId = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id.trim();
        if (movieRepo.existsById(movieId)) {
            throw new ConflictException("Movie " + movieId + " already exists");
        }
        return movieRepo.save(new Movie(movieId, title.trim(), language.trim(), durationMin, genre.trim()));
    }

    // Lists all movies.
    @Transactional(readOnly = true)
    public List<Movie> listMovies() {
        return movieRepo.findAll();
    }

    // Updates a movie's details.
    public Movie updateMovie(String id, String title, String language, int durationMin, String genre) {
        Movie movie = movieRepo.findById(id).orElseThrow(() -> NotFoundException.of("Movie", id));
        movie.update(title.trim(), language.trim(), durationMin, genre.trim());
        return movie;
    }

    // Deletes a movie; fails with a conflict if it still has shows.
    public void deleteMovie(String id) {
        Movie movie = movieRepo.findById(id).orElseThrow(() -> NotFoundException.of("Movie", id));
        movieRepo.delete(movie);
        movieRepo.flush();
    }

    private City findCity(int id) {
        return cityRepo.findById(id).orElseThrow(() -> NotFoundException.of("City", id));
    }

    private Theatre findTheatre(int id) {
        return theatreRepo.findById(id).orElseThrow(() -> NotFoundException.of("Theatre", id));
    }

    private Screen findScreen(int id) {
        return screenRepo.findById(id).orElseThrow(() -> NotFoundException.of("Screen", id));
    }
}
