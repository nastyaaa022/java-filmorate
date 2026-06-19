package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Collection<User> allUsers() {
        return userService.allUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable("id") Long userId) {
        return userService.getUserById(userId);
    }

    @PostMapping
    public User addUser(@RequestBody User user) {
        return userService.addUser(user);
    }

    @PutMapping
    public User userUpdate(@RequestBody User newUser) {
        return userService.userUpdate(newUser);
    }

    @PutMapping("/{id}/friends/{friendId}")
    public void addFriend(
            @PathVariable("id") Long userId,
            @PathVariable("friendId") Long friendId) {
        userService.addFriend(userId, friendId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public void removeFriend(
            @PathVariable("id") Long userId,
            @PathVariable("friendId") Long friendId) {
        userService.removeFriend(userId, friendId);
    }

    @GetMapping("/{id}/friends")
    public Collection<User> getUserFriends(@PathVariable("id") Long userId) {
      return userService.getUserFriends(userId);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public Collection<User> getCommonFriends(
            @PathVariable("id") Long userId,
            @PathVariable("otherId")Long otherId) {
        return userService.getCommonFriends(userId, otherId);
    }
}
