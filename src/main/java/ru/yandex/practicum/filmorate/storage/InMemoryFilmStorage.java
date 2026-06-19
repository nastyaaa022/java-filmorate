package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;

@Component
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films = new HashMap<>();
    private long nextId = 1;

    @Override
    public Film save(Film film) {
        if (film.getId() == null) {
            film.setId(nextId++);
            films.put(film.getId(), film);
        } else {
            films.put(film.getId(), film);
        }
        return film;
    }

    @Override
    public void delete(long id) {
        if (!films.containsKey(id)) {
            throw new NotFoundException("Фильм с ID " + id + " не найден");
        }
        films.remove(id);
    }


    @Override
    public Optional<Film> findById(long id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public List<Film> getAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public Film update(Film film) {
        if (!films.containsKey(film.getId())) {
            throw new NotFoundException("Фильм с ID " + film.getId() + " не найден");
        }
        films.put(film.getId(), film);
        return film;
    }
}
