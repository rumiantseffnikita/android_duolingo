-- ============================================================
-- LinguaAI — Исправление enum-типов для совместимости с PostgREST
-- Выполнить в Supabase SQL Editor если результаты уроков не сохраняются
-- ============================================================

-- Меняем enum-столбцы на TEXT/VARCHAR для совместимости с REST API

-- 1. difficulty_level: enum → VARCHAR
ALTER TABLE duolingo.users
    ALTER COLUMN difficulty_level DROP DEFAULT,
    ALTER COLUMN difficulty_level TYPE VARCHAR(20) USING difficulty_level::text,
    ALTER COLUMN difficulty_level SET DEFAULT 'beginner';

-- 2. exercise_type: enum → VARCHAR
ALTER TABLE duolingo.exercise_results
    ALTER COLUMN exercise_type DROP DEFAULT,
    ALTER COLUMN exercise_type TYPE VARCHAR(30) USING exercise_type::text,
    ALTER COLUMN exercise_type SET DEFAULT 'translation';

-- 3. Удаляем неиспользуемые enum-типы (опционально)
-- DROP TYPE IF EXISTS duolingo.difficulty_level;
-- DROP TYPE IF EXISTS duolingo.exercise_type;

-- 4. Перезагружаем кэш PostgREST
NOTIFY pgrst, 'reload schema';
