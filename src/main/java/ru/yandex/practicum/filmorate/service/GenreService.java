package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenreService {

    public List<Genre> getAllGenres() {
        log.info("Запрос списка всех жанров");
        return Arrays.asList(Genre.values());
    }

    public Genre getGenreById(int id) {
        log.info("Запрос жанра с id: {}", id);
        if (id < 1 || id > Genre.values().length) {
            log.warn("Жанр с id: {} не найден", id);
            throw new NotFoundException("Жанр с id " + id + " не найден.");
        }
        return Genre.values()[id - 1];
    }
}

