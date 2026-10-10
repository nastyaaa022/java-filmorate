package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.*;

@Service
public class FilmService {

    private final FilmStorage filmStorage;

    private final UserStorage userStorage;

    private final MpaService mpaService;

    private final GenreService genreService;

    private static final Logger log = LoggerFactory.getLogger(FilmService.class);

    public static final LocalDate FIRST_FILM_DATE = LocalDate.of(1895, 12, 28);

    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       MpaService mpaService,
                       GenreService genreService) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.mpaService = mpaService;
        this.genreService = genreService;
    }

    public Collection<Film> getAllFilms() {
        Collection<Film> allFilms = filmStorage.getAll();
        log.info("Запрос списка фильмов. Найдено фильмов: {}", allFilms.size());
        return allFilms;
    }

    public Film getFilmById(Long filmId) {
        Film film = getFilmOrThrow(filmId);

        log.info("Запрос фильма по ID: {}. Найден фильм '{}'", filmId, film.getName());
        return film;
    }

    public Film addFilm(Film film) {
        validateName(film.getName(), null, "создании");
        validateDescription(film.getDescription(), null, "создании");
        validateReleaseDate(film.getReleaseDate(), null, "создании");
        validateDuration(film.getDuration(), null, "создании");

        if (film.getMpa() == null) {
            throw new NotFoundException("Неверный ID рейтинга MPA");
        }
        checkMpaExistence(film.getMpa().getId());
        checkGenresExistence(film.getGenres());

        Film savedFilm = filmStorage.save(film);
        log.info("Фильм '{}' успешно добавлен, ID: {}", film.getName(), film.getId());
        return savedFilm;
    }

    public Film updateFilm(Film newFilm) {
        if (newFilm.getId() == null) {
            log.warn("Не указан Id фильма.");
            throw new ValidationException("Id должен быть указан");
        }

        Film oldFilm = getFilmOrThrow(newFilm.getId());

        String oldName = oldFilm.getName();
        String oldDescription = oldFilm.getDescription();
        LocalDate oldReleaseDate = oldFilm.getReleaseDate();
        Integer oldDuration = oldFilm.getDuration();

        updateFilmFields(newFilm, oldFilm);

        filmStorage.update(oldFilm);

        String updatedFields = getUpdatedFieldsString(newFilm, oldName, oldDescription,
                oldReleaseDate, oldDuration);

        log.info("Фильм '{}' успешно изменен, ID: {}. Обновлены поля: {}",
                oldFilm.getName(), newFilm.getId(), updatedFields);

        return oldFilm;
    }

    public void likeFilm(Long filmId, Long userId) {
        getFilmOrThrow(filmId);
        getUserOrThrow(userId);

        filmStorage.addLike(filmId, userId);
        log.info("Пользователь {} поставил лайк фильму (ID: {})", userId, filmId);
    }

    public void unlikeFilm(Long filmId, Long userId) {
        getFilmOrThrow(filmId);
        getUserOrThrow(userId);

        filmStorage.removeLike(filmId, userId);
        log.info("Пользователь {} удалил лайк у фильма (ID: {})", userId, filmId);
    }

    public Collection<Film> getTop10PopularFilms(int count) {
        if (count <= 0) {
            log.warn("Ошибка: запрос топ-{} популярных фильмов. Количество должно быть положительным.", count);
            throw new ValidationException("Количество фильмов должно быть положительным числом");
        }

        log.info("Запрос топ-{} популярных фильмов", count);
        return filmStorage.getTopPopular(count);
    }

    //вспомогательные методы
    private Film getFilmOrThrow(Long filmId) {
        return filmStorage.findById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id: " + filmId + " не найден."));
    }

    private User getUserOrThrow(Long userId) {
        return userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));
    }

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

    private void updateFilmFields(Film newFilm, Film oldFilm) {
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

        if (newFilm.getMpa() != null) {
            checkMpaExistence(newFilm.getMpa().getId()); // Используем общий метод 👈
            oldFilm.setMpa(newFilm.getMpa());
        }

        if (newFilm.getGenres() != null) {
            checkGenresExistence(newFilm.getGenres()); // Используем общий метод 👈
            oldFilm.setGenres(newFilm.getGenres());
        } else {
            oldFilm.setGenres(new java.util.LinkedHashSet<>());
        }
    }

    private void checkMpaExistence(int mpaId) {
        mpaService.getMpaById(mpaId);
    }

    private void checkGenresExistence(Collection<Genre> genres) {
        if (genres != null) {
            for (Genre genre : genres) {
                if (genre == null) {
                    throw new NotFoundException("Один из указанных жанров не существует.");
                }
                genreService.getGenreById(genre.getId());
            }
        }
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
        int maxDescription = 200;
        if (description != null) {
            int descriptionLength = description.length();
            if (descriptionLength > maxDescription) {
                log.warn(
                        "Ошибка размера описания при {} фильма с ID {}: длина: {} символов. Максимально допустимая длина: {}",
                        context, filmId, descriptionLength, maxDescription
                );
                throw new ValidationException("Длина описания не должна превышать 200 символов");
            }
        }
    }

    private void validateReleaseDate(LocalDate releaseDate, Long filmId, String context) {
        if (releaseDate == null) {
            log.warn("Ошибка валидации Даты при {} фильма с ID {}: дата релиза не может быть пустой", context, filmId);
            throw new ValidationException("дата релиза фильма не может быть пустой");
        }

        if (releaseDate.isBefore(FIRST_FILM_DATE)) {
            log.warn(
                    "Ошибка валидации Даты {} при {} фильма с ID {}: дата релиза не должна быть раньше 28 декабря 1895 года",
                    releaseDate, context, filmId
            );
            throw new ValidationException("дата релиза не должна быть раньше 28 декабря 1895 года");
        }
    }

    private void validateDuration(Integer duration, Long filmId, String context) {
        if (duration == null || duration <= 0) {
            log.warn(
                    "Ошибка продолжительности {}: при {} фильма с ID {}: продолжительность фильма должна быть положительным числом",
                    duration, context, filmId
            );
            throw new ValidationException("продолжительность фильма должна быть положительным числом");
        }
    }
}
