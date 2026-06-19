package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<Long, User> users = new HashMap<>();

    @GetMapping
    public Collection<User> allUsers() {
        Collection<User> allUsers = users.values();
        log.info("Запрос списка пользователей. Найдено пользователей: {}", allUsers.size());
        return users.values();
    }

    @PostMapping
    public User addUser(@RequestBody User user) {
        validateLogin(user.getLogin(), null, "создании");
        checkLoginUniqueness(user.getLogin(), null);
        validateEmail(user.getEmail(), null, "создании");
        validateBirthday(user.getBirthday(), null, "создании");

        String validatedEmail = getValidatedEmail(user, null, "создании");
        checkEmailUniqueness(validatedEmail, null);
        user.setEmail(validatedEmail);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        user.setId(getNextId());
        users.put(user.getId(), user);

        log.info("Пользователь '{}' успешно добавлен, ID: {}", user.getName(), user.getId());
        return user;
    }

    @PutMapping
    public User userUpdate(@RequestBody User newUser) {
        if (newUser.getId() == null) {
            log.warn("Не указан Id пользователя.");
            throw new ValidationException("Id должен быть указан");
        }

        if (!users.containsKey(newUser.getId())) {
            log.warn("Пользователь с id {} не найден. Запрос на обновление отклонён.", newUser.getId());
            throw new NotFoundException("Пользователь с id: " + newUser.getId() + " не найден");
        }

        User oldUser = users.get(newUser.getId());

        String oldName = oldUser.getName();
        String oldEmail = oldUser.getEmail();
        LocalDate oldBirthday = oldUser.getBirthday();
        String oldLogin = oldUser.getLogin();

        updateUserLogin(newUser, oldUser);
        String validatedEmail = updateUserEmail(newUser, oldUser);
        updateUserName(newUser, oldUser, oldName, oldLogin);
        updateUserBirthday(newUser, oldUser);

        String updatedFieldsStr = getUpdatedFieldsString(newUser, validatedEmail,
                oldName, oldEmail, oldBirthday, oldLogin);

        log.info("Пользователь '{}' успешно изменен, ID: {}. Обновлены поля: {}",
                oldUser.getName(), newUser.getId(), updatedFieldsStr);

        return oldUser;
    }

//методы для обновления данных
    private void updateUserName(User newUser, User oldUser, String oldName, String oldLogin) {
        if (newUser.getName() != null && !newUser.getName().isBlank()) {
            oldUser.setName(newUser.getName());
        } else {
            boolean wasNameMissing = oldName == null ||
                    oldName.isBlank() ||
                    oldName.equals(oldLogin);

            if (wasNameMissing) {
                oldUser.setName(oldUser.getLogin());
            } else {
                oldUser.setName(oldName);
            }
        }
    }

    private void updateUserLogin(User newUser, User oldUser) {
        if (newUser.getLogin() != null) {
            validateLogin(newUser.getLogin(), newUser.getId(), "обновлении");
            checkLoginUniqueness(newUser.getLogin(), newUser.getId());
            oldUser.setLogin(newUser.getLogin());
        }
    }

    private String updateUserEmail(User newUser, User oldUser) {
        String validatedEmail = null;
        if (newUser.getEmail() != null) {
            validatedEmail = getValidatedEmail(newUser, newUser.getId(), "обновлении");
            checkEmailUniqueness(validatedEmail, newUser.getId());
            oldUser.setEmail(validatedEmail);
        }
        return validatedEmail;
    }

    private void updateUserBirthday(User newUser, User oldUser) {
        if (newUser.getBirthday() != null) {
            validateBirthday(newUser.getBirthday(), newUser.getId(), "обновлении");
            oldUser.setBirthday(newUser.getBirthday());
        }
    }

//Методы валидации
    private void validateLogin(String login, Long userId, String context) {
        if (login == null || login.isBlank() || login.contains(" ")) {
            log.warn(
                    "Логин: {} невалиден при {} пользователя с ID {}: он пустой, состоит только из пробелов или содержит пробелы.",
                    login, context, userId
            );
            throw new ValidationException(
                    "Логин не может быть пустым, состоять только из пробелов или содержать пробелы при " + context);
        }
    }

    private void validateEmail(String email, Long userId, String operation) {
        if (email == null || email.trim().isEmpty()) {
            log.warn(
                    "Ошибка валидации email при {} пользователя с ID {}: не может быть null или пустым",
                    operation,
                    userId
            );
            throw new ValidationException("Email не может быть null или пустым при " + operation);
        }

        if (!email.contains("@")) {
            log.warn(
                    "Ошибка валидации email при {} пользователя с ID {}: email не содержит символ @",
                    operation,
                    userId
            );
            throw new ValidationException("Email должен содержать символ @ при " + operation);
        }
    }

    private void validateBirthday(LocalDate birthday, Long userId, String context) {
        if (birthday != null) {
            LocalDate now = LocalDate.now();
            if (birthday.isAfter(now)) {
                log.warn(
                        "Дата рождения: {} невалидна, при {} пользователя с ID {}: Она не может быть в будущем.",
                        birthday, context, userId
                );
                throw new ValidationException(
                        "Дата рождения не может быть в будущем при " + context
                );
            }
        }
    }

    private void checkEmailUniqueness(String email, Long currentId) {
        User existingUser = users.values().stream()
                .filter(user -> user.getEmail().equals(email) &&
                        (currentId == null || !user.getId().equals(currentId)))
                .findFirst()
                .orElse(null);

        if (existingUser != null) {
            log.warn(
                    "Email: {} уже занят пользователем с ID {}. Попытка присвоения пользователю с ID {}.",
                    email,
                    existingUser.getId(),
                    currentId
            );
            throw new DuplicatedDataException("Этот имейл уже используется");
        }
    }

    private void checkLoginUniqueness(String login, Long currentId) {
        User existingUser = users.values().stream()
                .filter(user -> user.getLogin().equals(login) &&
                        (currentId == null || !user.getId().equals(currentId)))
                .findFirst()
                .orElse(null);

        if (existingUser != null) {
            log.warn(
                    "Логин: {} уже занят пользователем с ID {}. Попытка присвоения пользователю с ID {}.",
                    login,
                    existingUser.getId(),
                    currentId
            );
            throw new DuplicatedDataException("Этот логин уже используется");
        }
    }

//Вспомогательные методы
    private String getValidatedEmail(User user, Long userId, String operation) {
        String email = user.getEmail();
        validateEmail(email, userId, operation);
        return email.trim();
    }

    private String getUpdatedFieldsString(User newUser, String validatedEmail, String oldName,
                                          String oldEmail, LocalDate oldBirthday, String oldLogin) {
        List<String> updatedFields = new ArrayList<>();

        if (newUser.getName() != null && !Objects.equals(newUser.getName(), oldName)) {
            updatedFields.add("name");
        }
        if (newUser.getEmail() != null && !Objects.equals(validatedEmail, oldEmail)) {
            updatedFields.add("email");
        }
        if (newUser.getBirthday() != null && !Objects.equals(newUser.getBirthday(), oldBirthday)) {
            updatedFields.add("birthday");
        }
        if (newUser.getLogin() != null && !Objects.equals(newUser.getLogin(), oldLogin)) {
            updatedFields.add("login");
        }

        return updatedFields.isEmpty() ? "никакие поля не обновлены" : String.join(", ", updatedFields);
    }

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);

        return ++currentMaxId;
    }
}
