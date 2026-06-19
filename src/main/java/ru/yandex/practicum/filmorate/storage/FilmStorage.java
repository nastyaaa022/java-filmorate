package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

public interface FilmStorage {
    Film save(Film film);                    // добавление/обновление объекта
    void delete(long id);                   // удаление по ID
    Optional<Film> findById(long id);       // поиск по ID
    List<Film> getAll();                    // получение всех объектов
    Film update(Film film);                 // модификация объекта
}
