package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films = new HashMap<>();
    private long nextId = 1;

    @Override
    public Film save(Film film) {
        film.setId(assignId(film.getId()));
        films.put(film.getId(), film);
        return film;
    }

    private long assignId(Long id) {
        if (id == null) {
            return nextId++;
        }
        if (id >= nextId) {
            nextId = id + 1;
        }
        return id;
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
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public List<Film> getTopPopular(int count) {
        return films.values().stream()
                .sorted((f1, f2) -> Long.compare(f2.getLikedByUsers().size(), f1.getLikedByUsers().size()))
                .limit(count)
                .collect(Collectors.toList());
    }
}
