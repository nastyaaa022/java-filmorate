# Filmorate

Приложение для поиска и оценки фильмов.

## ER-диаграмма базы данных

![ER-диаграмма Filmorate](er-diagram.png)

### Описание таблиц

| Таблица | Назначение |
|---------|-----------|
| **users** | Пользователи приложения |
| **films** | Фильмы |
| **mpa_rating** | Справочник возрастных рейтингов MPA |
| **genres** | Справочник жанров |
| **friendships** | Связь «дружба» между пользователями (M:N) |
| **film_likes** | Лайки фильмов от пользователей (M:N) |
| **film_genres** | Связь фильмов с жанрами (M:N) |

### Примеры SQL-запросов

### получить всех пользователей:
SELECT * 
FROM users;

### Получить пользователя по ID:

SELECT * 
FROM users 
WHERE id = ?;

### Получить всех друзей пользователя:
SELECT *
FROM users u
JOIN friendships f ON u.id=f.friend_id
WHERE f.user_id = ?;

### Получить общих друзей:
SELECT u.*
FROM users u
JOIN friendships f1 ON u.id = f1.friend_id AND f1.user_id = ?
JOIN friendships f2 ON u.id = f2.friend_id AND f2.user_id = ?;

### Получить все фильмы:
SELECT * 
FROM films;

### Получить фильм по ID:
SELECT * 
FROM films 
WHERE id = ?;

### Получить топ N популярных фильмов (по количеству лайков):
SELECT f.*, 
 COUNT(fl.user_id) AS likes_count
FROM films f
LEFT JOIN film_likes fl ON f.id = fl.film_id
GROUP BY f.id
ORDER BY likes_count DESC
LIMIT ?;

### Получить фильм с его жанрами:
SELECT f.*, 
       g.id AS genre_id, 
       g.name AS genre_name
FROM films f
LEFT JOIN film_genres fg ON f.id = fg.film_id
LEFT JOIN genres g ON fg.genre_id = g.id
WHERE f.id = ?;

### Получить фильм с его MPA рейтингом:
SELECT f.*, 
       mr.name AS mpa_name
FROM films f
JOIN mpa_rating mr ON f.mpa_id = mr.id
WHERE f.id = ?; 

