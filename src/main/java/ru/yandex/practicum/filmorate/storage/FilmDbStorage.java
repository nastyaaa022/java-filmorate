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
                stmt.setInt(5, film.getMpa().getId());
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

        film.setLikedByUsers(new HashSet<>());
        updateGenres(film);

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
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId());

        jdbcTemplate.update("DELETE FROM film_genres WHERE id_film = ?", film.getId());

        if (film.getGenres() == null) {
            film.setGenres(new LinkedHashSet<>());
        } else {
            film.setGenres(new LinkedHashSet<>(film.getGenres()));
        }

        updateGenres(film);
        film.setLikedByUsers(new HashSet<>(getLikesForFilm(film.getId())));

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
        String sqlFilms = "SELECT f.*, m.name AS mpa_name FROM films f LEFT JOIN mpa_rating m ON f.id_mpa_rating = m.id";
        List<Film> films = jdbcTemplate.query(sqlFilms, this::mapRowToFilm);

        if (films.isEmpty()) {
            return films;
        }

        loadGenresForFilms(films);
        loadLikesForFilms(films);

        return films;
    }

    @Override
    public List<Film> getTopPopular(int count) {
        String sqlFilms = "SELECT f.*, m.name AS mpa_name FROM films f LEFT JOIN mpa_rating m ON f.id_mpa_rating = m.id";
        List<Film> allFilms = jdbcTemplate.query(sqlFilms, this::mapRowToFilm);

        if (allFilms.isEmpty()) {
            return allFilms;
        }

        loadGenresForFilms(allFilms);
        loadLikesForFilms(allFilms);

        return allFilms.stream()
                .sorted((f1, f2) -> {
                    int likesCompare = Integer.compare(f2.getLikedByUsers().size(), f1.getLikedByUsers().size());
                    if (likesCompare != 0) {
                        return likesCompare;
                    }
                    return Long.compare(f1.getId(), f2.getId());
                })
                .limit(count)
                .collect(java.util.stream.Collectors.toList());
    }

    private List<Genre> getGenresForFilm(long filmId) {
        String sql = "SELECT id_genre FROM film_genres WHERE id_film = ? ORDER BY id_genre";
        return jdbcTemplate.query(sql, (rs, rowNum) -> Genre.values()[rs.getInt("id_genre") - 1], filmId);
    }

    private List<Long> getLikesForFilm(long filmId) {
        String sql = "SELECT id_user FROM film_likes WHERE id_film = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id_user"), filmId);
    }

    private void updateGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }
        List<Genre> uniqueGenres = new ArrayList<>(new LinkedHashSet<>(film.getGenres()));
        String sql = "INSERT INTO film_genres (id_film, id_genre) VALUES (?, ?)";

        jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Genre genre = uniqueGenres.get(i);
                ps.setLong(1, film.getId());
                ps.setInt(2, genre.ordinal() + 1);
            }

            @Override
            public int getBatchSize() {
                return uniqueGenres.size();
            }
        });
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = Film.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .releaseDate(rs.getDate("releaseDate").toLocalDate())
                .duration(rs.getInt("duration"))
                .build();

        int mpaId = rs.getInt("id_mpa_rating");
        if (mpaId > 0) {
            film.setMpa(MpaRating.values()[mpaId - 1]);
        }
        return film;
    }

    @Override
    public void addLike(long filmId, long userId) {
        String sql = "INSERT INTO film_likes (id_film, id_user) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(long filmId, long userId) {
        String sql = "DELETE FROM film_likes WHERE id_film = ? AND id_user = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    //вспомогательны методы для информации с БД
    private void loadGenresForFilms(List<Film> films) {
        String sqlGenres = "SELECT fg.id_film, fg.id_genre FROM film_genres fg ORDER BY fg.id_genre";
        Map<Long, Set<Genre>> genresMap = jdbcTemplate.query(sqlGenres, (rs) -> {
            Map<Long, Set<Genre>> map = new HashMap<>();
            while (rs.next()) {
                long filmId = rs.getLong("id_film");
                int genreId = rs.getInt("id_genre");
                Genre genre = Genre.values()[genreId - 1];
                map.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
            }
            return map;
        });

        for (Film film : films) {
            film.setGenres(genresMap.getOrDefault(film.getId(), new LinkedHashSet<>()));
        }
    }

    private void loadLikesForFilms(List<Film> films) {
        String sqlLikes = "SELECT fl.id_film, fl.id_user FROM film_likes fl";
        Map<Long, Set<Long>> likesMap = jdbcTemplate.query(sqlLikes, (rs) -> {
            Map<Long, Set<Long>> map = new HashMap<>();
            while (rs.next()) {
                long filmId = rs.getLong("id_film");
                long userId = rs.getLong("id_user");
                map.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
            }
            return map;
        });

        for (Film film : films) {
            film.setLikedByUsers(likesMap.getOrDefault(film.getId(), new HashSet<>()));
        }
    }
}


