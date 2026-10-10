package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Component("filmDbStorage")
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Film save(Film film) {
        String sql = "INSERT INTO films (name, description, releaseDate, duration, id_mpa_rating) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, Date.valueOf(film.getReleaseDate()));
            stmt.setInt(4, film.getDuration());
            if (film.getMpa() != null) {
                stmt.setInt(5, film.getMpa().ordinal() + 1);
            } else {
                stmt.setNull(5, java.sql.Types.INTEGER);
            }
            return stmt;
        }, keyHolder);

        film.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());

        if (film.getGenres() == null) {
            film.setGenres(new LinkedHashSet<>());
        } else {
            film.setGenres(new LinkedHashSet<>(film.getGenres()));
        }

        if (film.getLikedByUsers() == null) {
            film.setLikedByUsers(new HashSet<>());
        }

        updateGenresAndLikes(film);

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Genre> sortedGenres = new TreeSet<>(Comparator.comparingInt(Genre::ordinal));
            sortedGenres.addAll(film.getGenres());
            film.setGenres(new LinkedHashSet<>(sortedGenres));
        }
        return film;
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, releaseDate = ?, duration = ?, id_mpa_rating = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().ordinal() + 1 : null,
                film.getId());

        jdbcTemplate.update("DELETE FROM film_genres WHERE id_film = ?", film.getId());
        jdbcTemplate.update("DELETE FROM film_likes WHERE id_film = ?", film.getId());

        if (film.getGenres() == null) {
            film.setGenres(new LinkedHashSet<>());
        } else {
            film.setGenres(new LinkedHashSet<>(film.getGenres()));
        }

        if (film.getLikedByUsers() == null) {
            film.setLikedByUsers(new HashSet<>());
        }

        updateGenresAndLikes(film);

        return film;
    }

    @Override
    public void delete(long id) {
        jdbcTemplate.update("DELETE FROM films WHERE id = ?", id);
    }

    @Override
    public Optional<Film> findById(long id) {
        String sql = "SELECT f.*, m.name AS mpa_name FROM films f LEFT JOIN mpa_rating m ON f.id_mpa_rating = m.id WHERE f.id = ?";
        try {
            Film film = jdbcTemplate.queryForObject(sql, this::mapRowToFilm, id);
            if (film != null) {
                film.setGenres(new LinkedHashSet<>(getGenresForFilm(id)));
                film.setLikedByUsers(new HashSet<>(getLikesForFilm(id)));
            }
            return Optional.ofNullable(film);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Film> getAll() {
        String sql = "SELECT f.*, m.name AS mpa_name FROM films f LEFT JOIN mpa_rating m ON f.id_mpa_rating = m.id";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm);
        for (Film film : films) {
            film.setGenres(new LinkedHashSet<>(getGenresForFilm(film.getId())));
            film.setLikedByUsers(new HashSet<>(getLikesForFilm(film.getId())));
        }
        return films;
    }

    @Override
    public List<Film> getTopPopular(int count) {
        String sql = "SELECT f.*, m.name AS mpa_name FROM films f LEFT JOIN mpa_rating m ON f.id_mpa_rating = m.id";
        List<Film> allFilms = jdbcTemplate.query(sql, this::mapRowToFilm);

        for (Film film : allFilms) {
            film.setGenres(new LinkedHashSet<>(getGenresForFilm(film.getId())));
            film.setLikedByUsers(new HashSet<>(getLikesForFilm(film.getId())));
        }

        return allFilms.stream()
                .sorted((f1, f2) -> {
                    int likesCompare = Integer.compare(f2.getLikedByUsers().size(), f1.getLikedByUsers().size());
                    if (likesCompare != 0) {
                        return likesCompare;
                    }
                    return Long.compare(f1.getId(), f2.getId());
                })
                .limit(count)
                .collect(Collectors.toList());
    }

    // Вспомогательный метод для сохранения жанров и лайков
    private void updateGenresAndLikes(Film film) {
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Genre> uniqueGenres = new HashSet<>(film.getGenres());
            for (Genre genre : uniqueGenres) {
                int databaseGenreId = genre.ordinal() + 1;
                jdbcTemplate.update("INSERT INTO film_genres (id_film, id_genre) VALUES (?, ?)",
                        film.getId(), databaseGenreId);
            }
        }

        if (film.getLikedByUsers() != null && !film.getLikedByUsers().isEmpty()) {
            Set<Long> uniqueLikes = new HashSet<>(film.getLikedByUsers());
            for (Long userId : uniqueLikes) {
                jdbcTemplate.update("INSERT INTO film_likes (id_film, id_user) VALUES (?, ?)",
                        film.getId(), userId);
            }
        }
    }

    private List<Genre> getGenresForFilm(long filmId) {
        String sql = "SELECT id_genre FROM film_genres WHERE id_film = ? ORDER BY id_genre";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            int genreIdFromDb = rs.getInt("id_genre");
            return Genre.values()[genreIdFromDb - 1];
        }, filmId);
    }

    private List<Long> getLikesForFilm(long filmId) {
        String sql = "SELECT id_user FROM film_likes WHERE id_film = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id_user"), filmId);
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        MpaRating mpa = null;
        int mpaId = rs.getInt("id_mpa_rating");
        if (!rs.wasNull()) {
            mpa = MpaRating.values()[mpaId - 1];
        }

        return Film.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .releaseDate(rs.getDate("releaseDate").toLocalDate())
                .duration(rs.getInt("duration"))
                .mpa(mpa)
                .genres(new HashSet<>())
                .likedByUsers(new HashSet<>())
                .build();
    }

    @Override
    public void addLike(long filmId, long userId) {
        jdbcTemplate.update("DELETE FROM film_likes WHERE id_film = ? AND id_user = ?", filmId, userId);
        String sql = "INSERT INTO film_likes (id_film, id_user) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(long filmId, long userId) {
        String sql = "DELETE FROM film_likes WHERE id_film = ? AND id_user = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }
}

