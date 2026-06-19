package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {
    private final FilmService filmService;

    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping
    public Collection<Film> getAllFilms() {
        return filmService.getAllFilms();
    }

    @GetMapping("/{id}")
    public Film getFilmById(@PathVariable("id") Long filmId) {
        return filmService.getFilmById(filmId);
    }

    @PostMapping
    public Film addFilm(@RequestBody Film film) {
        return filmService.addFilm(film);
    }

    @PutMapping
    public Film updateFilm(@RequestBody Film newFilm) {
        return filmService.updateFilm(newFilm);
    }

    @PutMapping("/{id}/like/{userId}")
    public void likeFilm(@PathVariable("id") Long filmId, @PathVariable("userId") Long userId) {
        filmService.likeFilm(filmId, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void unlikeFilm(@PathVariable("id") Long filmId, @PathVariable("userId") Long userId) {
        filmService.unlikeFilm(filmId, userId);
    }

    @GetMapping("/popular")
    public Collection<Film> getTop10PopularFilms(
            @RequestParam(defaultValue = "10") int count) {
        return filmService.getTop10PopularFilms(count);
    }
}



