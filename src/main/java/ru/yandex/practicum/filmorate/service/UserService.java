package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserStorage userStorage;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public Collection<User> allUsers() {
        Collection<User> allUsers = userStorage.getAll();
        log.info("Запрос списка пользователей. Найдено пользователей: {}", allUsers.size());
        return allUsers;
    }

    public User getUserById(Long userId) {
        User user = userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id: " + userId + " не найден."));
        log.info("Запрос пользователя по ID: {}. Найден пользователь '{}'", userId, user.getName());
        return user;
    }

    public User addUser(User user) {
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

        User savedUser = userStorage.save(user);

        log.info("Пользователь '{}' успешно добавлен, ID: {}", user.getName(), user.getId());
        return savedUser;
    }

    public User userUpdate(User newUser) {
        if (newUser.getId() == null) {
            log.warn("Не указан Id пользователя.");
            throw new ValidationException("Id должен быть указан");
        }

        User oldUser = getUserById(newUser.getId());

        String oldName = oldUser.getName();
        String oldEmail = oldUser.getEmail();
        LocalDate oldBirthday = oldUser.getBirthday();
        String oldLogin = oldUser.getLogin();

        updateUserLogin(newUser, oldUser);
        String validatedEmail = updateUserEmail(newUser, oldUser);
        updateUserName(newUser, oldUser, oldName, oldLogin);
        updateUserBirthday(newUser, oldUser);

        userStorage.save(oldUser);
        String updatedFieldsStr = getUpdatedFieldsString(newUser, validatedEmail,
                oldName, oldEmail, oldBirthday, oldLogin);

        log.info("Пользователь '{}' успешно изменен, ID: {}. Обновлены поля: {}",
                oldUser.getName(), newUser.getId(), updatedFieldsStr);

        return oldUser;
    }

    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить самого себя в друзья");
        }

        User user = getUserById(userId);
        User friend = getUserById(friendId);

        if (user.getFriends().contains(friendId)) {
            throw new ValidationException("Пользователь уже является другом");
        }

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        userStorage.save(user);
        userStorage.save(friend);

        log.info("Пользователь {} и пользователь {} теперь друзья", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя удалить самого себя из друзей");
        }

        User user = getUserById(userId);
        User friend = getUserById(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        userStorage.save(user);
        userStorage.save(friend);

        log.info("Пользователь {} и пользователь {} больше не друзья", userId, friendId);
    }

    public Collection<User> getUserFriends(Long userId) {
        log.info("Запрос списка друзей пользователя {}", userId);

        User user = getUserById(userId);
        return user.getFriends().stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        log.info("Запрос общих друзей пользователей {} и {}", userId, otherId);

        User user = getUserById(userId);
        User other = getUserById(otherId);

        Set<Long> commonIds = new HashSet<>(user.getFriends());
        commonIds.retainAll(other.getFriends());

        return commonIds.stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
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
        Optional<User> existingUserOpt = userStorage.findByEmail(email);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (currentId == null || !currentId.equals(existingUser.getId())) {
                log.warn(
                        "Email: {} уже занят пользователем с ID {}. Попытка присвоения пользователю с ID {}.",
                        email,
                        existingUser.getId(),
                        currentId
                );
                throw new DuplicatedDataException("Этот имейл уже используется");
            }
        }
    }

    private void checkLoginUniqueness(String login, Long currentId) {
        Optional<User> existingUserOpt = userStorage.findByLogin(login);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (currentId == null || !currentId.equals(existingUser.getId())) {
                log.warn(
                        "Логин: {} уже занят пользователем с ID {}. Попытка присвоения пользователю с ID {}.",
                        login,
                        existingUser.getId(),
                        currentId
                );
                throw new DuplicatedDataException("Этот логин уже используется");
            }
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
}
