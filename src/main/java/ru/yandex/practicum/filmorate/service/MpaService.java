package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpaService {
    private final JdbcTemplate jdbcTemplate;

    public List<MpaRating> getAllMpa() {
        log.info("Запрос списка всех рейтингов MPA");
        return Arrays.asList(MpaRating.values());
    }

    public MpaRating getMpaById(int id) {
        log.info("Запрос рейтинга MPA с id: {}", id);
        if (id < 1 || id > MpaRating.values().length) {
            log.warn("Рейтинг MPA с id: {} не найден", id);
            throw new NotFoundException("Рейтинг MPA с id " + id + " не найден.");
        }
        return MpaRating.values()[id - 1];
    }
}

