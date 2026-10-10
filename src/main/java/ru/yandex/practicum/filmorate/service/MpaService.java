package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.MpaStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpaService {

    private final MpaStorage mpaStorage;

    public List<MpaRating> getAllMpa() {
        log.info("Запрос списка всех рейтингов MPA из базы данных");
        return mpaStorage.getAll();
    }

    public MpaRating getMpaById(int id) {
        log.info("Запрос рейтинга MPA по ID из базы данных: {}", id);

        return mpaStorage.findById(id)
                .orElseThrow(() -> {
                    log.warn("Рейтинг MPA с id: {} не найден в базе данных", id);
                    return new NotFoundException("Рейтинг MPA с id " + id + " не найден.");
                });
    }
}


