package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

public interface FilmStorage {

    Film save(Film film);

    void delete(long id);

    Optional<Film> findById(long id);

    List<Film> getAll();

    Film update(Film film);

    List<Film> getTopPopular(int count);
}
