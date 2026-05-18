package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {
    private final Map<Long, Film> films = new HashMap<>();

    @GetMapping
    public Collection<Film> getAllFilms() {
        return films.values();
    }

    @PostMapping
    public Film addFilm(@RequestBody Film film) {
        validateName(film.getName(), null, "создании");
        validateDescription(film.getDescription(), null, "создании");
        validateReleaseDate(film.getReleaseDate(), null, "создании");
        validateDuration(film.getDuration(), null, "создании");

        film.setId(getNextId());
        films.put(film.getId(), film);

        log.info("Фильм '{}' успешно добавлен, ID: {}", film.getName(), film.getId());
        return film;
    }

    @PutMapping
    public Film updateFilm(@RequestBody Film newFilm) {
        if (newFilm.getId() == null) {
            log.warn("Не указан Id фильма.");
            throw new ValidationException("Id должен быть указан");
        }

        if (!films.containsKey(newFilm.getId())) {
            log.warn("Фильм с id {} не найден. Запрос на обновление отклонён.", newFilm.getId());
            throw new NotFoundException("Фильм с id = " + newFilm.getId() + " не найден");
        }

        Film oldFilm = films.get(newFilm.getId());

        // Сохранение старых значений для проверки на изменения полей
        String oldName = oldFilm.getName();
        String oldDescription = oldFilm.getDescription();
        LocalDate oldReleaseDate = oldFilm.getReleaseDate();
        Integer oldDuration = oldFilm.getDuration();

        // Обновление полей
        if (newFilm.getName() != null) {
            validateName(newFilm.getName(), newFilm.getId(), "обновлении");
            oldFilm.setName(newFilm.getName());
        }

        if (newFilm.getDescription() != null) {
            validateDescription(newFilm.getDescription(), newFilm.getId(), "обновлении");
            oldFilm.setDescription(newFilm.getDescription());
        }

        if (newFilm.getReleaseDate() != null) {
            validateReleaseDate(newFilm.getReleaseDate(), newFilm.getId(), "обновлении");
            oldFilm.setReleaseDate(newFilm.getReleaseDate());
        }

        if (newFilm.getDuration() != null) {
            validateDuration(newFilm.getDuration(), newFilm.getId(), "обновлении");
            oldFilm.setDuration(newFilm.getDuration());
        }

        //для лог о измененных полях
        String updatedFields = getUpdatedFieldsString(newFilm, oldName, oldDescription,
                oldReleaseDate, oldDuration);

        log.info("Фильм '{}' успешно изменен, ID: {}. Обновлены поля: {}",
                oldFilm.getName(), newFilm.getId(), updatedFields);

        return oldFilm;
    }

//вспомогательные методы
    private String getUpdatedFieldsString(Film newFilm, String oldName, String oldDescription,
                                          LocalDate oldReleaseDate, Integer oldDuration) {
        List<String> updatedFields = new ArrayList<>();

        if (newFilm.getName() != null && !Objects.equals(newFilm.getName(), oldName)) {
            updatedFields.add("name");
        }
        if (newFilm.getDescription() != null && !Objects.equals(newFilm.getDescription(), oldDescription)) {
            updatedFields.add("description");
        }
        if (newFilm.getReleaseDate() != null && !Objects.equals(newFilm.getReleaseDate(), oldReleaseDate)) {
            updatedFields.add("releaseDate");
        }
        if (newFilm.getDuration() != null && !Objects.equals(newFilm.getDuration(), oldDuration)) {
            updatedFields.add("duration");
        }

        return updatedFields.isEmpty() ? "никакие поля не обновлены" : String.join(", ", updatedFields);
    }

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);

        return ++currentMaxId;
    }

//методы валидации
    private void validateName(String name, Long filmId, String context) {
        if (name == null || name.isBlank()) {
            log.warn("Ошибка валидации имени: {}, при {} фильма с ID {}: название фильма не может быть пустым.",
                    name, context, filmId);
            throw new ValidationException("название фильма не может быть пустым.");
        }
    }

    private void validateDescription(String description, Long filmId, String context) {
        if (description == null || description.isBlank()) {
            log.warn("Ошибка описания фильма {}, при {} фильма c ID {}: описание фильма не может быть пустым.",
                    description, context, filmId);
            throw new ValidationException("описание фильма не может быть пустым");
        }

        int maxDescription = 200;
        int descriptionLength = description.length();
        if (descriptionLength > maxDescription) {
            log.warn("Ошибка размера описания при {} фильма с ID {}: длина: {} символов. Максимально допустимая длина: {}",
                    context, filmId, descriptionLength, maxDescription);
            throw new ValidationException("Длина описания не должна превышать 200 символов");
        }
    }

    private void validateReleaseDate(LocalDate releaseDate, Long filmId, String context) {
        if (releaseDate == null) {
            log.warn("Ошибка валидации Даты {} при {} фильма с ID {}: дата релиза не может быть пустой",
                    releaseDate, context, filmId);
            throw new ValidationException("дата релиза фильма не может быть пустой");
        } else {
            LocalDate minDate = LocalDate.of(1895, 12, 28);
            if (releaseDate.isBefore(minDate)) {
                log.warn("Ошибка валидации Даты {} при {} фильма с ID {}: дата релиза не должна быть раньше 28 декабря 1895 года",
                        releaseDate, context, filmId);
                throw new ValidationException("дата релиза не должна быть раньше 28 декабря 1895 года");
            }
        }
    }

    private void validateDuration(Integer duration, Long filmId, String context) {
        if (duration == null || duration <= 0) {
            log.warn("Ошибка продолжительности {}: при {} фильма с ID {}: продолжительность фильма должна быть положительным числом",
                    duration, context, filmId);
            throw new ValidationException("продолжительность фильма должна быть положительным числом");
        }
    }
}



