package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Optional;

public interface UserStorage {
    User save(User user);
    void delete(long id);
    Optional<User> findById(long id);
    List<User> getAll();
    User update(User user);

    Optional<User> findByEmail(String email);
    Optional<User> findByLogin(String login);
}
