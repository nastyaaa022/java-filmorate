package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.UserDbStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({UserDbStorage.class, FilmDbStorage.class})
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Sql(scripts = {"/schema.sql", "/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FilmorateApplicationTests {

	private final UserDbStorage userStorage;
	private final FilmDbStorage filmStorage;

	@Test
	public void testFindUserById() {
		User testUser = createTestUser("Test User", "login1", "test1@email.com");
		userStorage.save(testUser);

		Optional<User> userOptional = userStorage.findById(testUser.getId());

		assertThat(userOptional)
				.isPresent()
				.hasValueSatisfying(user ->
						assertThat(user).hasFieldOrPropertyWithValue("id", testUser.getId())
				);
	}

	@Test
	public void testGetAllUsers() {
		User user1 = createTestUser("User One", "login1", "one@email.com");
		User user2 = createTestUser("User Two", "login2", "two@email.com");
		userStorage.save(user1);
		userStorage.save(user2);

		List<User> users = userStorage.getAll();

		assertThat(users)
				.isNotNull()
				.hasSize(2);
	}

	@Test
	public void testUpdateUser() {
		User testUser = createTestUser("Initial Name", "login1", "test1@email.com");
		userStorage.save(testUser);

		testUser.setName("Updated Name");
		userStorage.update(testUser);

		Optional<User> updatedUserOptional = userStorage.findById(testUser.getId());

		assertThat(updatedUserOptional)
				.isPresent()
				.hasValueSatisfying(user ->
						assertThat(user).hasFieldOrPropertyWithValue("name", "Updated Name")
				);
	}

	@Test
	public void testDeleteUser() {
		User testUser = createTestUser("To Delete", "deleteMe", "delete@email.com");
		userStorage.save(testUser);

		userStorage.delete(testUser.getId());

		Optional<User> deletedUserOptional = userStorage.findById(testUser.getId());
		assertThat(deletedUserOptional).isEmpty();
	}

	@Test
	public void testFindByEmail() {
		User testUser = createTestUser("Email Guy", "emailGuy", "unique@email.com");
		userStorage.save(testUser);

		Optional<User> userOptional = userStorage.findByEmail("unique@email.com");

		assertThat(userOptional)
				.isPresent()
				.hasValueSatisfying(user ->
						assertThat(user).hasFieldOrPropertyWithValue("email", "unique@email.com")
				);
	}

	@Test
	public void testFindByLogin() {
		User testUser = createTestUser("Login Guy", "uniqueLogin", "login@email.com");
		userStorage.save(testUser);

		Optional<User> userOptional = userStorage.findByLogin("uniqueLogin");

		assertThat(userOptional)
				.isPresent()
				.hasValueSatisfying(user ->
						assertThat(user).hasFieldOrPropertyWithValue("login", "uniqueLogin")
				);
	}

	@Test
	public void testAddAndGetFriends() {
		User user = createTestUser("User", "userLog", "user@email.com");
		User friend = createTestUser("Friend", "friendLog", "friend@email.com");
		userStorage.save(user);
		userStorage.save(friend);

		userStorage.addFriend(user.getId(), friend.getId());

		Collection<User> friends = userStorage.getFriends(user.getId());

		assertThat(friends)
				.isNotNull()
				.hasSize(1)
				.extracting(User::getId)
				.contains(friend.getId());
	}

	@Test
	public void testRemoveFriend() {
		User user = createTestUser("User", "userLog", "user@email.com");
		User friend = createTestUser("Friend", "friendLog", "friend@email.com");
		userStorage.save(user);
		userStorage.save(friend);

		userStorage.addFriend(user.getId(), friend.getId());
		userStorage.removeFriend(user.getId(), friend.getId());

		Collection<User> friends = userStorage.getFriends(user.getId());
		assertThat(friends).isEmpty();
	}

	@Test
	public void testSaveAndFindFilmById() {
		Film testFilm = createTestFilm("Inception", "Dream thief", 148, MpaRating.PG_13);
		filmStorage.save(testFilm);

		Optional<Film> filmOptional = filmStorage.findById(testFilm.getId());

		assertThat(filmOptional)
				.isPresent()
				.hasValueSatisfying(film -> {
					assertThat(film).hasFieldOrPropertyWithValue("id", testFilm.getId());
					assertThat(film).hasFieldOrPropertyWithValue("name", "Inception");
				});
	}

	@Test
	public void testUpdateFilm() {
		Film testFilm = createTestFilm("Old Title", "Description", 100, MpaRating.G);
		filmStorage.save(testFilm);

		testFilm.setName("New Title");
		filmStorage.update(testFilm);

		Optional<Film> updatedFilm = filmStorage.findById(testFilm.getId());

		assertThat(updatedFilm)
				.isPresent()
				.hasValueSatisfying(film ->
						assertThat(film).hasFieldOrPropertyWithValue("name", "New Title")
				);
	}

	@Test
	public void testGetAllFilms() {
		Film film1 = createTestFilm("Film One", "Desc 1", 90, MpaRating.R);
		Film film2 = createTestFilm("Film Two", "Desc 2", 120, MpaRating.PG);
		filmStorage.save(film1);
		filmStorage.save(film2);

		List<Film> films = filmStorage.getAll();

		assertThat(films)
				.isNotNull()
				.hasSize(2);
	}

	// Вспомогательные методы создания объектов для тестов
	private User createTestUser(String name, String login, String email) {
		return User.builder()
				.name(name)
				.login(login)
				.email(email)
				.birthday(LocalDate.of(2000, 1, 1))
				.build();
	}

	private Film createTestFilm(String name, String description, int duration, MpaRating mpa) {
		return Film.builder()
				.name(name)
				.description(description)
				.duration(duration)
				.releaseDate(LocalDate.of(2010, 7, 16))
				.mpa(mpa)
				.build();
	}
}




