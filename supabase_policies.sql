-- ============================================================
-- LinguaAI — RLS-политики для Supabase
-- Выполнить в Supabase SQL Editor ПОСЛЕ supabase_schema.sql
-- ============================================================

-- ============================================================
-- 1. Удаляем старые "allow_all" политики
-- ============================================================

DROP POLICY IF EXISTS "allow_all_users" ON duolingo.users;
DROP POLICY IF EXISTS "allow_all_languages" ON duolingo.languages;
DROP POLICY IF EXISTS "allow_all_topics" ON duolingo.topics;
DROP POLICY IF EXISTS "allow_all_words" ON duolingo.words;
DROP POLICY IF EXISTS "allow_all_word_progresses" ON duolingo.word_progresses;
DROP POLICY IF EXISTS "allow_all_learning_sessions" ON duolingo.learning_sessions;
DROP POLICY IF EXISTS "allow_all_exercise_results" ON duolingo.exercise_results;
DROP POLICY IF EXISTS "allow_all_achievements" ON duolingo.achievements;
DROP POLICY IF EXISTS "allow_all_user_achievements" ON duolingo.user_achievements;

-- ============================================================
-- 2. Убеждаемся что RLS включён на всех таблицах
-- ============================================================

ALTER TABLE duolingo.languages ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.topics ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.words ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.word_progresses ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.learning_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.exercise_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.achievements ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.user_achievements ENABLE ROW LEVEL SECURITY;

-- ============================================================
-- 3. LANGUAGES — справочная таблица
--    Чтение: все (anon + authenticated)
--    Запись: только service_role (через Dashboard / миграции)
-- ============================================================

CREATE POLICY "languages_select_all"
    ON duolingo.languages FOR SELECT
    USING (true);

CREATE POLICY "languages_insert_service"
    ON duolingo.languages FOR INSERT
    WITH CHECK (current_setting('role') = 'service_role');

CREATE POLICY "languages_update_service"
    ON duolingo.languages FOR UPDATE
    USING (current_setting('role') = 'service_role')
    WITH CHECK (current_setting('role') = 'service_role');

CREATE POLICY "languages_delete_service"
    ON duolingo.languages FOR DELETE
    USING (current_setting('role') = 'service_role');

-- ============================================================
-- 4. USERS — пользователи
--    Регистрация (INSERT): все (anon) — чтобы можно было создать аккаунт
--    Чтение (SELECT): все (для проверки email при логине/регистрации)
--    Обновление (UPDATE): все (приложение управляет авторизацией)
--    Удаление (DELETE): только service_role
-- ============================================================

CREATE POLICY "users_select_all"
    ON duolingo.users FOR SELECT
    USING (true);

CREATE POLICY "users_insert_all"
    ON duolingo.users FOR INSERT
    WITH CHECK (true);

CREATE POLICY "users_update_all"
    ON duolingo.users FOR UPDATE
    USING (true)
    WITH CHECK (true);

CREATE POLICY "users_delete_service"
    ON duolingo.users FOR DELETE
    USING (current_setting('role') = 'service_role');

-- ============================================================
-- 5. TOPICS — темы уроков (справочная таблица)
--    Чтение: все
--    Запись: только service_role
-- ============================================================

CREATE POLICY "topics_select_all"
    ON duolingo.topics FOR SELECT
    USING (true);

CREATE POLICY "topics_insert_service"
    ON duolingo.topics FOR INSERT
    WITH CHECK (current_setting('role') = 'service_role');

CREATE POLICY "topics_update_service"
    ON duolingo.topics FOR UPDATE
    USING (current_setting('role') = 'service_role')
    WITH CHECK (current_setting('role') = 'service_role');

CREATE POLICY "topics_delete_service"
    ON duolingo.topics FOR DELETE
    USING (current_setting('role') = 'service_role');

-- ============================================================
-- 6. WORDS — слова (справочная таблица)
--    Чтение: все
--    Запись: только service_role
-- ============================================================

CREATE POLICY "words_select_all"
    ON duolingo.words FOR SELECT
    USING (true);

CREATE POLICY "words_insert_service"
    ON duolingo.words FOR INSERT
    WITH CHECK (current_setting('role') = 'service_role');

CREATE POLICY "words_update_service"
    ON duolingo.words FOR UPDATE
    USING (current_setting('role') = 'service_role')
    WITH CHECK (current_setting('role') = 'service_role');

CREATE POLICY "words_delete_service"
    ON duolingo.words FOR DELETE
    USING (current_setting('role') = 'service_role');

-- ============================================================
-- 7. WORD_PROGRESSES — прогресс по словам
--    Чтение: все (приложение фильтрует по user_id)
--    Создание: все (при изучении нового слова)
--    Обновление: все (при повторении слова)
--    Удаление: только service_role
-- ============================================================

CREATE POLICY "word_progresses_select_all"
    ON duolingo.word_progresses FOR SELECT
    USING (true);

CREATE POLICY "word_progresses_insert_all"
    ON duolingo.word_progresses FOR INSERT
    WITH CHECK (true);

CREATE POLICY "word_progresses_update_all"
    ON duolingo.word_progresses FOR UPDATE
    USING (true)
    WITH CHECK (true);

CREATE POLICY "word_progresses_delete_service"
    ON duolingo.word_progresses FOR DELETE
    USING (current_setting('role') = 'service_role');

-- ============================================================
-- 8. LEARNING_SESSIONS — сессии обучения
--    Чтение: все
--    Создание: все (при начале урока)
--    Обновление: все (при завершении урока)
--    Удаление: только service_role
-- ============================================================

CREATE POLICY "learning_sessions_select_all"
    ON duolingo.learning_sessions FOR SELECT
    USING (true);

CREATE POLICY "learning_sessions_insert_all"
    ON duolingo.learning_sessions FOR INSERT
    WITH CHECK (true);

CREATE POLICY "learning_sessions_update_all"
    ON duolingo.learning_sessions FOR UPDATE
    USING (true)
    WITH CHECK (true);

CREATE POLICY "learning_sessions_delete_service"
    ON duolingo.learning_sessions FOR DELETE
    USING (current_setting('role') = 'service_role');

-- ============================================================
-- 9. EXERCISE_RESULTS — результаты упражнений
--    Чтение: все
--    Создание: все (при ответе на упражнение)
--    Обновление/Удаление: только service_role
-- ============================================================

CREATE POLICY "exercise_results_select_all"
    ON duolingo.exercise_results FOR SELECT
    USING (true);

CREATE POLICY "exercise_results_insert_all"
    ON duolingo.exercise_results FOR INSERT
    WITH CHECK (true);

CREATE POLICY "exercise_results_update_service"
    ON duolingo.exercise_results FOR UPDATE
    USING (current_setting('role') = 'service_role')
    WITH CHECK (current_setting('role') = 'service_role');

CREATE POLICY "exercise_results_delete_service"
    ON duolingo.exercise_results FOR DELETE
    USING (current_setting('role') = 'service_role');

-- ============================================================
-- 10. ACHIEVEMENTS — список достижений (справочная таблица)
--     Чтение: все
--     Запись: только service_role
-- ============================================================

CREATE POLICY "achievements_select_all"
    ON duolingo.achievements FOR SELECT
    USING (true);

CREATE POLICY "achievements_insert_service"
    ON duolingo.achievements FOR INSERT
    WITH CHECK (current_setting('role') = 'service_role');

CREATE POLICY "achievements_update_service"
    ON duolingo.achievements FOR UPDATE
    USING (current_setting('role') = 'service_role')
    WITH CHECK (current_setting('role') = 'service_role');

CREATE POLICY "achievements_delete_service"
    ON duolingo.achievements FOR DELETE
    USING (current_setting('role') = 'service_role');

-- ============================================================
-- 11. USER_ACHIEVEMENTS — достижения пользователя
--     Чтение: все
--     Создание: все (при получении достижения)
--     Обновление/Удаление: только service_role
-- ============================================================

CREATE POLICY "user_achievements_select_all"
    ON duolingo.user_achievements FOR SELECT
    USING (true);

CREATE POLICY "user_achievements_insert_all"
    ON duolingo.user_achievements FOR INSERT
    WITH CHECK (true);

CREATE POLICY "user_achievements_update_service"
    ON duolingo.user_achievements FOR UPDATE
    USING (current_setting('role') = 'service_role')
    WITH CHECK (current_setting('role') = 'service_role');

CREATE POLICY "user_achievements_delete_service"
    ON duolingo.user_achievements FOR DELETE
    USING (current_setting('role') = 'service_role');

-- ============================================================
-- 12. Права доступа на схему (на случай если не были выданы)
-- ============================================================

GRANT USAGE ON SCHEMA duolingo TO anon, authenticated, service_role;
GRANT ALL ON ALL TABLES IN SCHEMA duolingo TO anon, authenticated, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA duolingo TO anon, authenticated, service_role;
