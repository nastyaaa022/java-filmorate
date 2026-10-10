package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(UserDbStorage.class)
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Sql(scripts = {"/schema.sql", "/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FilmorateApplicationTests {

	private final UserDbStorage userStorage;

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

		List<Long> friendsIds = userStorage.getFriendsIds(user.getId());

		assertThat(friendsIds)
				.isNotNull()
				.hasSize(1)
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

		List<Long> friendsIds = userStorage.getFriendsIds(user.getId());
		assertThat(friendsIds).isEmpty();
	}

	// Вспомогательный метод
	private User createTestUser(String name, String login, String email) {
		return User.builder()
				.name(name)
				.login(login)
				.email(email)
				.birthday(LocalDate.of(2000, 1, 1))
				.build();
	}
}



