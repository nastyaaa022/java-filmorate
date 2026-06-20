package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmControllerTest {

    private FilmController controller;
    private FilmService service;
    private FilmStorage storage;

    private UserStorage storageUser;

    @BeforeEach
    void setUp() {
        storage = new InMemoryFilmStorage();
        storageUser = new InMemoryUserStorage();
        service = new FilmService(storage, storageUser);
        controller = new FilmController(service);
    }

    //позитивные тесты
    @Test
    void addFilm_ValidData_ShouldAddSuccessfully() {
        Film film = Film.builder()
                .name("Inception")
                .description("A thief who steals corporate secrets through dream-sharing technology.")
                .releaseDate(LocalDate.of(2010, 7, 16))
                .duration(148)
                .build();

        Film addedFilm = controller.addFilm(film);

        assertNotNull(addedFilm);
        assertNotNull(addedFilm.getId());
        assertEquals("Inception", addedFilm.getName());
        assertEquals(148, addedFilm.getDuration());
        assertEquals(LocalDate.of(2010, 7, 16), addedFilm.getReleaseDate());
    }

    @Test
    void addFilm_NameIsNull_ShouldSetNameFromLogin() {
        Film film = Film.builder()
                .name(null)
                .description("Test")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addFilm(film)
        );

        assertTrue(exception.getMessage().contains("название фильма не может быть пустым"));
    }

    @Test
    void updateFilm_FullUpdate_ShouldUpdateAllFields() {
        Film originalFilm = Film.builder()
                .name("Old Name")
                .description("Old description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        Film addedFilm = controller.addFilm(originalFilm);
        Long filmId = addedFilm.getId();

        Film updateRequest = Film.builder()
                .id(filmId)
                .name("New Name")
                .description("New description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(200)
                .build();

        Film updatedFilm = controller.updateFilm(updateRequest);

        assertEquals(filmId, updatedFilm.getId());
        assertEquals("New Name", updatedFilm.getName());
        assertEquals("New description", updatedFilm.getDescription());
        assertEquals(LocalDate.of(2020, 1, 1), updatedFilm.getReleaseDate());
        assertEquals(200, updatedFilm.getDuration());
    }

    @Test
    void updateFilm_PartialUpdate_ShouldUpdateOnlyProvidedFields() {
        Film originalFilm = Film.builder()
                .name("Original Name")
                .description("Original description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        Film addedFilm = controller.addFilm(originalFilm);
        Long filmId = addedFilm.getId();

        Film updateRequest = Film.builder()
                .id(filmId)
                .name("Updated Name")
                .duration(150)
                .build();

        Film updatedFilm = controller.updateFilm(updateRequest);

        assertEquals("Updated Name", updatedFilm.getName());
        assertEquals(150, updatedFilm.getDuration());
        assertEquals("Original description", updatedFilm.getDescription());
        assertEquals(LocalDate.of(2000, 1, 1), updatedFilm.getReleaseDate());
    }

    @Test
    void getAllFilms_EmptyList_ShouldReturnEmptyCollection() {
        Collection<Film> films = controller.getAllFilms();

        assertNotNull(films);
        assertTrue(films.isEmpty());
    }

    @Test
    void getAllFilms_MultipleFilms_ShouldReturnAllFilms() {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Description 1")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        Film film2 = Film.builder()
                .name("Film 2")
                .description("Description 2")
                .releaseDate(LocalDate.of(2010, 1, 1))
                .duration(120)
                .build();

        controller.addFilm(film1);
        controller.addFilm(film2);

        Collection<Film> films = controller.getAllFilms();
        List<Film> filmList = List.copyOf(films);

        assertEquals(2, filmList.size());
        assertTrue(filmList.stream().anyMatch(f -> f.getName().equals("Film 1")));
        assertTrue(filmList.stream().anyMatch(f -> f.getName().equals("Film 2")));
    }

    // негативные тесты валидации

    @Test
    void addFilm_EmptyName_ShouldThrowValidationException() {
        Film film = Film.builder()
                .name(" ")
                .description("Test description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addFilm(film)
        );

        assertTrue(exception.getMessage().contains("название фильма не может быть пустым"));
    }

    @Test
    void addFilm_DescriptionTooLong_ShouldThrowValidationException() {
        String longDescription = "A".repeat(201); // 201 символов

        Film film = Film.builder()
                .name("Test Film")
                .description(longDescription)
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addFilm(film)
        );

        assertTrue(exception.getMessage().contains("Длина описания не должна превышать 200 символов"));
    }

    @Test
    void addFilm_ReleaseDateBeforeMinDate_ShouldThrowValidationException() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Test description")
                .releaseDate(LocalDate.of(1895, 12, 27))
                .duration(100)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addFilm(film)
        );

        assertTrue(exception.getMessage().contains("дата релиза не должна быть раньше 28 декабря 1895 года"));
    }

    @Test
    void addFilm_NullReleaseDate_ShouldThrowValidationException() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Test description")
                .releaseDate(null)
                .duration(100)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addFilm(film)
        );

        assertTrue(exception.getMessage().contains("дата релиза фильма не может быть пустой"));
    }

    @Test
    void addFilm_ZeroDuration_ShouldThrowValidationException() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Test description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(0)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addFilm(film)
        );

        assertTrue(exception.getMessage().contains("продолжительность фильма должна быть положительным числом"));
    }

    @Test
    void addFilm_NegativeDuration_ShouldThrowValidationException() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Test description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(-10)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addFilm(film)
        );

        assertTrue(exception.getMessage().contains("продолжительность фильма должна быть положительным числом"));
    }

    @Test
    void addFilm_NullDuration_ShouldThrowValidationException() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Test description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(null)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.addFilm(film)
        );

        assertTrue(exception.getMessage().contains("продолжительность фильма должна быть положительным числом"));
    }

    @Test
    void updateFilm_NullId_ShouldThrowValidationException() {
        Film film = Film.builder()
                .id(null)
                .name("Updated Name")
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.updateFilm(film)
        );

        assertTrue(exception.getMessage().contains("Id должен быть указан"));
    }

    @Test
    void updateFilm_NonExistentId_ShouldThrowNotFoundException() {
        Film film = Film.builder()
                .id(999L)
                .name("Updated Name")
                .build();

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> controller.updateFilm(film)
        );

        assertTrue(exception.getMessage().contains("Фильм с id = 999 не найден"));
    }

    @Test
    void updateFilm_InvalidDataInUpdate_ShouldThrowValidationException() {
        Film originalFilm = Film.builder()
                .name("Original")
                .description("Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        Film addedFilm = controller.addFilm(originalFilm);

        Film invalidUpdate = Film.builder()
                .id(addedFilm.getId())
                .name(" ")
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.updateFilm(invalidUpdate)
        );

        assertTrue(exception.getMessage().contains("название фильма не может быть пустым"));
    }

    // тесты граничных значений

    @Test
    void addFilm_DescriptionExactly200Chars_ShouldAddSuccessfully() {
        String exactDescription = "A".repeat(200); // ровно 200 символов

        Film film = Film.builder()
                .name("Test Film")
                .description(exactDescription)
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        Film addedFilm = controller.addFilm(film);

        assertNotNull(addedFilm);
        assertEquals(exactDescription, addedFilm.getDescription());
    }

    @Test
    void addFilm_ReleaseDateExactlyMinDate_ShouldAddSuccessfully() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Test description")
                .releaseDate(LocalDate.of(1895, 12, 28)) // минимальная допустимая дата
                .duration(100)
                .build();

        Film addedFilm = controller.addFilm(film);

        assertNotNull(addedFilm);
        assertEquals(LocalDate.of(1895, 12, 28), addedFilm.getReleaseDate());
    }

    @Test
    void addFilm_DurationOneMinute_ShouldAddSuccessfully() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Test description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(1) // минимальная положительная продолжительность
                .build();

        Film addedFilm = controller.addFilm(film);

        assertNotNull(addedFilm);
        assertEquals(1, addedFilm.getDuration());
    }

    //тесты обновления полей

    @Test
    void updateFilm_NoFieldsChanged_ShouldLogNoFieldsUpdated() {
        Film originalFilm = Film.builder()
                .name("Test Film")
                .description("Test description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        Film addedFilm = controller.addFilm(originalFilm);

        Film sameData = Film.builder()
                .id(addedFilm.getId())
                .name("Test Film")
                .description("Test description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        Film updatedFilm = controller.updateFilm(sameData);

        assertEquals("Test Film", updatedFilm.getName());
        assertEquals("Test description", updatedFilm.getDescription());
        assertEquals(100, updatedFilm.getDuration());
    }

    @Test
    void updateFilm_SingleFieldChanged_ShouldLogOnlyThatField() {
        Film originalFilm = Film.builder()
                .name("Original Name")
                .description("Original description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        Film addedFilm = controller.addFilm(originalFilm);

        Film updateRequest = Film.builder()
                .id(addedFilm.getId())
                .name("New Name")
                .build();

        Film updatedFilm = controller.updateFilm(updateRequest);

        assertEquals("New Name", updatedFilm.getName());
        assertEquals("Original description", updatedFilm.getDescription());
        assertEquals(100, updatedFilm.getDuration());
    }

    @Test
    void updateFilm_MultipleFieldsChanged_ShouldLogAllChangedFields() {
        Film originalFilm = Film.builder()
                .name("Original")
                .description("Original description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        Film addedFilm = controller.addFilm(originalFilm);

        Film updateRequest = Film.builder()
                .id(addedFilm.getId())
                .name("Updated Name")
                .duration(200)
                .build();

        Film updatedFilm = controller.updateFilm(updateRequest);

        assertEquals("Updated Name", updatedFilm.getName());
        assertEquals(200, updatedFilm.getDuration());
        assertEquals("Original description", updatedFilm.getDescription());
    }

    @Test
    void updateFilm_FieldSetToSameValue_ShouldNotCountAsUpdate() {
        Film originalFilm = Film.builder()
                .name("Test Film")
                .description("Test description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        Film addedFilm = controller.addFilm(originalFilm);

        Film updateRequest = Film.builder()
                .id(addedFilm.getId())
                .name("Test Film")
                .description("New description")
                .build();

        Film updatedFilm = controller.updateFilm(updateRequest);

        assertEquals("Test Film", updatedFilm.getName());
        assertEquals("New description", updatedFilm.getDescription());
    }

   //тест на генерацию ID

    @Test
    void getIdGeneration_ShouldGenerateUniqueIncrementalIds() {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Description 1")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        Film film2 = Film.builder()
                .name("Film 2")
                .description("Description 2")
                .releaseDate(LocalDate.of(2001, 1, 1))
                .duration(120)
                .build();

        Film addedFilm1 = controller.addFilm(film1);
        Film addedFilm2 = controller.addFilm(film2);

        assertNotNull(addedFilm1.getId());
        assertNotNull(addedFilm2.getId());
        assertEquals(addedFilm1.getId() + 1, addedFilm2.getId());
    }
}

