package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class UserControllerTest {

    private UserController controller;
    private UserService service;
    private UserStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryUserStorage();
        service = new UserService(storage);
        controller = new UserController(service);
    }

    @Test
    void contextLoads() {
        // Тест загрузки контекста Spring
    }

    //тесты создания пользователя

    @Test
    void addUser_ValidData_ShouldAddSuccessfully() {
        User newUser = User.builder()
                .name("Test User")
                .login("testuser")
                .email("test@mail.com")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        User addedUser = controller.addUser(newUser);

        assertNotNull(addedUser);
        assertNotNull(addedUser.getId());
        assertEquals("Test User", addedUser.getName());
        assertEquals("testuser", addedUser.getLogin());
        assertEquals("test@mail.com", addedUser.getEmail());
        assertEquals(LocalDate.of(2000, 10, 5), addedUser.getBirthday());
    }

    @Test
    void addUser_NullName_ShouldUseLoginAsName() {
        User newUser = User.builder()
                .name(null)
                .login("user1")
                .email("user1@mail.r")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        User addedUser = controller.addUser(newUser);

        assertNotNull(addedUser);
        assertEquals("user1", addedUser.getName());
    }

    @Test
    void addUser_EmptyName_ShouldUseLoginAsName() {
        User newUser = User.builder()
                .name("")
                .login("user1")
                .email("user1@mail.r")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        User addedUser = controller.addUser(newUser);

        assertNotNull(addedUser);
        assertEquals("user1", addedUser.getName());
    }

    @Test
    void addUser_BlankName_ShouldUseLoginAsName() {
        User newUser = User.builder()
                .name("   ")
                .login("user1")
                .email("user1@mail.r")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        User addedUser = controller.addUser(newUser);

        assertNotNull(addedUser);
        assertEquals("user1", addedUser.getName());
    }

    @Test
    void addUser_NullEmail_ShouldThrowValidationException() {
        User newUser = User.builder()
                .name(null)
                .login("user1")
                .email(null)
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addUser(newUser)
        );

        assertTrue(exception.getMessage().contains("Email не может быть null или пустым при "));
    }

    @Test
    void addUser_EmptyEmail_ShouldThrowValidationException() {
        User newUser = User.builder()
                .name(null)
                .login("user1")
                .email("")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addUser(newUser)
        );

        assertTrue(exception.getMessage().contains("Email не может быть null или пустым при "));
    }

    @Test
    void addUser_BlankEmail_ShouldThrowValidationException() {
        User newUser = User.builder()
                .name(null)
                .login("user1")
                .email("   ")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addUser(newUser)
        );

        assertTrue(exception.getMessage().contains("Email не может быть null или пустым при "));
    }

    @Test
    void addUser_EmailWithoutAtSymbol_ShouldThrowValidationException() {
        User newUser = User.builder()
                .name(null)
                .login("user1")
                .email("usermail")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addUser(newUser)
        );

        assertTrue(exception.getMessage().contains("Email должен содержать символ @ при "));
    }

    @Test
    void addUser_EmailWithSpaces_ShouldBeTrimmed() {
        User newUser = User.builder()
                .name("Test")
                .login("user1")
                .email("  test@mail.com  ")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        User addedUser = controller.addUser(newUser);

        assertNotNull(addedUser);
        assertEquals("test@mail.com", addedUser.getEmail());
    }

    @Test
    void addUser_NullLogin_ShouldThrowValidationException() {
        User newUser = User.builder()
                .name("Test")
                .login(null)
                .email("test@mail.com")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addUser(newUser)
        );

        assertTrue(exception.getMessage().contains("Логин не может быть пустым"));
    }

    @Test
    void addUser_EmptyLogin_ShouldThrowValidationException() {
        User newUser = User.builder()
                .name("null")
                .login("")
                .email("user@mail")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addUser(newUser)
        );

        assertTrue(exception.getMessage().contains(
                "Логин не может быть пустым, состоять только из пробелов или содержать пробелы при "
        ));
    }

    @Test
    void addUser_BlankLogin_ShouldThrowValidationException() {
        User newUser = User.builder()
                .name("null")
                .login("   ")
                .email("user@mail")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addUser(newUser)
        );

        assertTrue(exception.getMessage().contains(
                "Логин не может быть пустым, состоять только из пробелов или содержать пробелы при "
        ));
    }

    @Test
    void addUser_LoginWithSpaces_ShouldThrowValidationException() {
        User newUser = User.builder()
                .name("Test")
                .login("user 1")
                .email("test@mail.com")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addUser(newUser)
        );

        assertTrue(exception.getMessage().contains("Логин не может быть пустым, состоять только из пробелов или содержать пробелы при "));
    }

    @Test
    void addUser_FutureBirthday_ShouldThrowValidationException() {
        User newUser = User.builder()
                .name("null")
                .login("user1")
                .email("user@mail")
                .birthday(LocalDate.of(3000, 10, 5))
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addUser(newUser)
        );
        assertTrue(exception.getMessage().contains("Дата рождения не может быть в будущем"));
    }

    @Test
    void addUser_TodayBirthday_ShouldAddSuccessfully() {
        User newUser = User.builder()
                .name("Test")
                .login("user1")
                .email("test@mail.com")
                .birthday(LocalDate.now())
                .build();

        User addedUser = controller.addUser(newUser);

        assertNotNull(addedUser);
        assertEquals(LocalDate.now(), addedUser.getBirthday());
    }

    @Test
    void addUser_PastBirthday_ShouldAddSuccessfully() {
        User newUser = User.builder()
                .name("Test")
                .login("user1")
                .email("test@mail.com")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User addedUser = controller.addUser(newUser);

        assertNotNull(addedUser);
        assertEquals(LocalDate.of(1990, 1, 1), addedUser.getBirthday());
    }

    // тесты обновления пользователя

    @Test
    void updateUser_EmptyNameWhenNameWasSameAsLogin_ShouldSetNameToLogin() {
        User originalUser = User.builder()
                .name(null)
                .login("ivan")
                .email("ivan@mail.com")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User addedUser = controller.addUser(originalUser);

        User updateRequest = User.builder()
                .id(addedUser.getId())
                .name("")
                .build();

        User updatedUser = controller.userUpdate(updateRequest);

        assertEquals("ivan", updatedUser.getName());
        assertEquals("ivan", updatedUser.getLogin());
    }

    @Test
    void updateUser_EmptyNameWhenNameWasDifferentFromLogin_ShouldKeepOldName() {
        User originalUser = User.builder()
                .name("Иван Иванов")
                .login("ivan")
                .email("ivan@mail.com")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User addedUser = controller.addUser(originalUser);

        User updateRequest = User.builder()
                .id(addedUser.getId())
                .name("")
                .build();

        User updatedUser = controller.userUpdate(updateRequest);

        assertEquals("Иван Иванов", updatedUser.getName());
        assertEquals("ivan", updatedUser.getLogin());
    }

    @Test
    void updateUser_FullUpdate_ShouldUpdateAllFields() {
        User originalUser = User.builder()
                .name("Original Name")
                .login("original")
                .email("original@mail.com")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User addedUser = controller.addUser(originalUser);
        Long userId = addedUser.getId();

        User updateRequest = User.builder()
                .id(userId)
                .name("Updated Name")
                .login("updated")
                .email("updated@mail.com")
                .birthday(LocalDate.of(2001, 1, 1))
                .build();

        User updatedUser = controller.userUpdate(updateRequest);

        assertEquals(userId, updatedUser.getId());
        assertEquals("Updated Name", updatedUser.getName());
        assertEquals("updated", updatedUser.getLogin());
        assertEquals("updated@mail.com", updatedUser.getEmail());
        assertEquals(LocalDate.of(2001, 1, 1), updatedUser.getBirthday());
    }

    @Test
    void updateUser_PartialUpdate_ShouldUpdateOnlyProvidedFields() {
        User originalUser = User.builder()
                .name("Katya")
                .login("user1")
                .email("katya@mail")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        User addedUser = controller.addUser(originalUser);

        User updatedUser = User.builder()
                .id(addedUser.getId())
                .email("newemail@mail")
                .build();

        User result = controller.userUpdate(updatedUser);

        assertEquals("newemail@mail", result.getEmail());
        assertEquals("Katya", result.getName());
        assertEquals("user1", result.getLogin());
        assertEquals(LocalDate.of(2000, 10, 5), result.getBirthday());
    }

    @Test
    void updateUser_UpdateName_ShouldUpdateOnlyName() {
        User originalUser = User.builder()
                .name("null")
                .login("user1")
                .email("user@mail")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        User addedUser = controller.addUser(originalUser);

        User upUser = User.builder()
                .id(addedUser.getId())
                .name("Katya")
                .login("user1")
                .email("user@mail")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        User upUserResult = controller.userUpdate(upUser);

        assertEquals("Katya", upUserResult.getName(), "Имя пользователя не было обновлено");
        assertEquals("user@mail", upUserResult.getEmail());
    }

    @Test
    void updateUser_UpdateEmail_ShouldUpdateOnlyEmail() {
        User newUser = User.builder()
                .name("Katya")
                .login("user1")
                .email("user@mail")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        User addedUser = controller.addUser(newUser);

        User upUser = User.builder()
                .id(addedUser.getId())
                .name("Katya")
                .login("user1")
                .email("katya@mail")
                .build();

        User upUserResult = controller.userUpdate(upUser);

        assertNotNull(addedUser);
        assertNotNull(upUserResult);

        assertEquals("katya@mail", upUserResult.getEmail(), "Email не был обновлен");
        assertEquals("Katya", upUserResult.getName());
        assertEquals(LocalDate.of(2000, 10, 5), upUserResult.getBirthday());
    }

    @Test
    void updateUser_UpdateLogin_ShouldUpdateOnlyLogin() {
        User originalUser = User.builder()
                .name("Test")
                .login("oldlogin")
                .email("test@mail.com")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User addedUser = controller.addUser(originalUser);

        User updateRequest = User.builder()
                .id(addedUser.getId())
                .login("newlogin")
                .build();

        User updatedUser = controller.userUpdate(updateRequest);

        assertEquals("newlogin", updatedUser.getLogin());
        assertEquals("Test", updatedUser.getName());
        assertEquals("test@mail.com", updatedUser.getEmail());
    }

    @Test
    void updateUser_UpdateBirthday_ShouldUpdateOnlyBirthday() {
        User originalUser = User.builder()
                .name("Test")
                .login("user1")
                .email("test@mail.com")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User addedUser = controller.addUser(originalUser);

        User updateRequest = User.builder()
                .id(addedUser.getId())
                .birthday(LocalDate.of(2001, 1, 1))
                .build();

        User updatedUser = controller.userUpdate(updateRequest);

        assertEquals(LocalDate.of(2001, 1, 1), updatedUser.getBirthday());
        assertEquals("Test", updatedUser.getName());
        assertEquals("test@mail.com", updatedUser.getEmail());
    }

    @Test
    void updateUser_NonExistentId_ShouldThrowNotFoundException() {
        User upUser = User.builder()
                .id(2L)
                .name("Katya")
                .build();

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> controller.userUpdate(upUser)
        );
        assertTrue(exception.getMessage().contains("Пользователь с id: 2 не найден"));
    }

    @Test
    void updateUser_NullId_ShouldThrowValidationException() {
        User upUser = User.builder()
                .id(null)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.userUpdate(upUser)
        );
        assertTrue(exception.getMessage().contains("Id должен быть указан"));
    }

    @Test
    void updateUser_DuplicateEmail_ShouldThrowDuplicatedDataException() {
        User user1 = User.builder()
                .name("Katya")
                .login("user1")
                .email("katya@mail")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        User user2 = User.builder()
                .name("Ivan")
                .login("user2")
                .email("ivan@mail")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        controller.addUser(user1);
        controller.addUser(user2);

        User updatedUser = User.builder()
                .id(user1.getId())
                .email("ivan@mail")
                .build();

        DuplicatedDataException exception = assertThrows(
                DuplicatedDataException.class,
                () -> controller.userUpdate(updatedUser)
        );
        assertTrue(exception.getMessage().contains("Этот имейл уже используется"));
    }

    @Test
    void updateUser_DuplicateLogin_ShouldThrowDuplicatedDataException() {
        User user1 = User.builder()
                .name("Katya")
                .login("user1")
                .email("katya@mail")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        User user2 = User.builder()
                .name("Ivan")
                .login("user2")
                .email("ivan@mail")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        controller.addUser(user1);
        controller.addUser(user2);

        User updatedUser = User.builder()
                .id(user1.getId())
                .login("user2")
                .build();

        DuplicatedDataException exception = assertThrows(
                DuplicatedDataException.class,
                () -> controller.userUpdate(updatedUser)
        );
        assertTrue(exception.getMessage().contains("Этот логин уже используется"));
    }

    @Test
    void updateUser_SameEmail_ShouldNotThrowException() {
        User originalUser = User.builder()
                .name("Test")
                .login("user1")
                .email("test@mail.com")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User addedUser = controller.addUser(originalUser);

        User updateRequest = User.builder()
                .id(addedUser.getId())
                .email("test@mail.com")
                .build();

        User updatedUser = controller.userUpdate(updateRequest);

        assertEquals("test@mail.com", updatedUser.getEmail());
    }

    @Test
    void updateUser_SameLogin_ShouldNotThrowException() {
        User originalUser = User.builder()
                .name("Test")
                .login("user1")
                .email("test@mail.com")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User addedUser = controller.addUser(originalUser);

        User updateRequest = User.builder()
                .id(addedUser.getId())
                .login("user1")
                .build();

        User updatedUser = controller.userUpdate(updateRequest);

        assertEquals("user1", updatedUser.getLogin());
    }

    // тесты на уникальные данные

    @Test
    void addUser_DuplicateLogin_ShouldThrowDuplicatedDataException() {
        User user1 = User.builder()
                .name("Katya")
                .login("user1")
                .email("katya@mail")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        User user2 = User.builder()
                .name("Anna")
                .login("user1")
                .email("anna@mail")
                .birthday(LocalDate.of(2001, 1, 1))
                .build();

        controller.addUser(user1);

        DuplicatedDataException exception = assertThrows(
                DuplicatedDataException.class,
                () -> controller.addUser(user2)
        );
        assertTrue(exception.getMessage().contains("Этот логин уже используется"));
    }

    @Test
    void addUser_DuplicateEmail_ShouldThrowDuplicatedDataException() {
        User user1 = User.builder()
                .name("Katya")
                .login("user1")
                .email("same@mail.com")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        User user2 = User.builder()
                .name("Anna")
                .login("user2")
                .email("same@mail.com")
                .birthday(LocalDate.of(2001, 1, 1))
                .build();

        controller.addUser(user1);

        DuplicatedDataException exception = assertThrows(
                DuplicatedDataException.class,
                () -> controller.addUser(user2)
        );
        assertTrue(exception.getMessage().contains("Этот имейл уже используется"));
    }

    // тесты получение данных

    @Test
    void getAllUsers_EmptyList_ShouldReturnEmptyCollection() {
        Collection<User> users = controller.allUsers();

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    void getAllUsers_MultipleUsers_ShouldReturnAllUsers() {
        User user1 = User.builder()
                .name("Katya")
                .login("user1")
                .email("user@mail")
                .birthday(LocalDate.of(2000, 10, 5))
                .build();

        User user2 = User.builder()
                .name("Ivan")
                .login("user2")
                .email("user2@mail")
                .birthday(LocalDate.of(2000, 12, 4))
                .build();

        User addedUser1 = controller.addUser(user1);
        User addedUser2 = controller.addUser(user2);

        List<User> allUsers = new ArrayList<>(controller.allUsers());

        assertEquals(2, allUsers.size(), "Ожидаем 2 пользователя в списке");

        assertTrue(allUsers.contains(addedUser1), "Первый добавленный пользователь не найден в списке");
        assertTrue(allUsers.contains(addedUser2), "Второй добавленный пользователь не найден в списке");
    }

    // тест на проверку ID

    @Test
    void getIdGeneration_ShouldGenerateUniqueIncrementalIds() {
        User user1 = User.builder()
                .name("User 1")
                .login("user1")
                .email("user1@mail.com")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User user2 = User.builder()
                .name("User 2")
                .login("user2")
                .email("user2@mail.com")
                .birthday(LocalDate.of(2001, 1, 1))
                .build();

        User user3 = User.builder()
                .name("User 3")
                .login("user3")
                .email("user3@mail.com")
                .birthday(LocalDate.of(2002, 1, 1))
                .build();

        User addedUser1 = controller.addUser(user1);
        User addedUser2 = controller.addUser(user2);
        User addedUser3 = controller.addUser(user3);

        assertNotNull(addedUser1.getId());
        assertNotNull(addedUser2.getId());
        assertNotNull(addedUser3.getId());

        assertEquals(addedUser1.getId() + 1, addedUser2.getId());
        assertEquals(addedUser2.getId() + 1, addedUser3.getId());
    }

    // тесты на обновления полей

    @Test
    void updateUser_NoFieldsChanged_ShouldLogNoFieldsUpdated() {
        User originalUser = User.builder()
                .name("Test User")
                .login("testuser")
                .email("test@mail.com")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User addedUser = controller.addUser(originalUser);

        User sameData = User.builder()
                .id(addedUser.getId())
                .name("Test User")
                .login("testuser")
                .email("test@mail.com")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User updatedUser = controller.userUpdate(sameData);

        assertEquals("Test User", updatedUser.getName());
        assertEquals("testuser", updatedUser.getLogin());
        assertEquals("test@mail.com", updatedUser.getEmail());
        assertEquals(LocalDate.of(2000, 1, 1), updatedUser.getBirthday());
    }

    @Test
    void updateUser_SingleFieldChanged_ShouldLogOnlyThatField() {
        User originalUser = User.builder()
                .name("Original Name")
                .login("original")
                .email("original@mail.com")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User addedUser = controller.addUser(originalUser);

        User updateRequest = User.builder()
                .id(addedUser.getId())
                .name("New Name")
                .build();

        User updatedUser = controller.userUpdate(updateRequest);

        assertEquals("New Name", updatedUser.getName());
        assertEquals("original", updatedUser.getLogin());
        assertEquals("original@mail.com", updatedUser.getEmail());
    }

    @Test
    void updateUser_MultipleFieldsChanged_ShouldLogAllChangedFields() {
        User originalUser = User.builder()
                .name("Original")
                .login("original")
                .email("original@mail.com")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User addedUser = controller.addUser(originalUser);

        User updateRequest = User.builder()
                .id(addedUser.getId())
                .name("Updated Name")
                .email("updated@mail.com")
                .build();

        User updatedUser = controller.userUpdate(updateRequest);

        assertEquals("Updated Name", updatedUser.getName());
        assertEquals("updated@mail.com", updatedUser.getEmail());
        assertEquals("original", updatedUser.getLogin());
        assertEquals(LocalDate.of(2000, 1, 1), updatedUser.getBirthday()); // не изменилось
    }

    @Test
    void updateUser_FieldSetToSameValue_ShouldNotCountAsUpdate() {
        User originalUser = User.builder()
                .name("Test User")
                .login("testuser")
                .email("test@mail.com")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User addedUser = controller.addUser(originalUser);

        User updateRequest = User.builder()
                .id(addedUser.getId())
                .name("Test User")
                .email("new@mail.com")
                .build();

        User updatedUser = controller.userUpdate(updateRequest);

        assertEquals("Test User", updatedUser.getName());
        assertEquals("new@mail.com", updatedUser.getEmail());
    }

}

