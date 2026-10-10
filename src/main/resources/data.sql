-- Заполняем рейтинги MPA (используем MERGE, чтобы данные не дублировались при перезапуске)
MERGE INTO mpa_rating KEY(id) VALUES (1, 'G');
MERGE INTO mpa_rating KEY(id) VALUES (2, 'PG');
MERGE INTO mpa_rating KEY(id) VALUES (3, 'PG-13');
MERGE INTO mpa_rating KEY(id) VALUES (4, 'R');
MERGE INTO mpa_rating KEY(id) VALUES (5, 'NC-17');

-- Заполняем жанры
MERGE INTO genres KEY(id) VALUES (1, 'Комедия');
MERGE INTO genres KEY(id) VALUES (2, 'Драма');
MERGE INTO genres KEY(id) VALUES (3, 'Мультфильм');
MERGE INTO genres KEY(id) VALUES (4, 'Триллер');
MERGE INTO genres KEY(id) VALUES (5, 'Документальный');
MERGE INTO genres KEY(id) VALUES (6, 'Боевик');
