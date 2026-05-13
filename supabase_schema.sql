-- ============================================================
-- LinguaAI — SQL-схема для Supabase
-- Выполнить в Supabase SQL Editor (Dashboard → SQL Editor → New query)
-- ============================================================

-- 1. Создаём схему
CREATE SCHEMA IF NOT EXISTS duolingo;

-- 2. Расширение для генерации UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 3. Enum-типы
CREATE TYPE duolingo.difficulty_level AS ENUM ('beginner', 'intermediate', 'advanced');
CREATE TYPE duolingo.exercise_type AS ENUM ('translation', 'listening', 'speaking', 'multiple_choice', 'fill_blank');

-- ============================================================
-- ТАБЛИЦЫ
-- ============================================================

-- Языки
CREATE TABLE duolingo.languages (
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(10) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    native_name VARCHAR(100) NOT NULL,
    flag_emoji  VARCHAR(10),
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Пользователи
CREATE TABLE duolingo.users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(200) NOT NULL,
    email               VARCHAR(200) NOT NULL UNIQUE,
    password_hash       VARCHAR(500) NOT NULL,
    native_language_id  INT REFERENCES duolingo.languages(id),
    target_language_id  INT REFERENCES duolingo.languages(id),
    difficulty_level    duolingo.difficulty_level DEFAULT 'beginner',
    daily_goal_minutes  INT DEFAULT 15,
    daily_goal_words    INT DEFAULT 10,
    streak_days         INT DEFAULT 0,
    longest_streak      INT DEFAULT 0,
    total_xp            INT DEFAULT 0,
    last_activity_date  DATE,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- Темы
CREATE TABLE duolingo.topics (
    id          SERIAL PRIMARY KEY,
    language_id INT NOT NULL REFERENCES duolingo.languages(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    icon_emoji  VARCHAR(10),
    sort_order  INT DEFAULT 0
);

-- Слова
CREATE TABLE duolingo.words (
    id                  SERIAL PRIMARY KEY,
    language_id         INT NOT NULL REFERENCES duolingo.languages(id) ON DELETE CASCADE,
    topic_id            INT REFERENCES duolingo.topics(id) ON DELETE SET NULL,
    word                VARCHAR(300) NOT NULL,
    translation         VARCHAR(300) NOT NULL,
    transcription       VARCHAR(300),
    example_sentence    TEXT,
    example_translation TEXT,
    frequency_rank      INT,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

-- Прогресс по словам (spaced repetition)
CREATE TABLE duolingo.word_progresses (
    id              SERIAL PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES duolingo.users(id) ON DELETE CASCADE,
    word_id         INT NOT NULL REFERENCES duolingo.words(id) ON DELETE CASCADE,
    repetitions     INT DEFAULT 0,
    easiness_factor DOUBLE PRECISION DEFAULT 2.5,
    interval_days   INT DEFAULT 1,
    next_review     DATE DEFAULT CURRENT_DATE,
    last_review     DATE,
    correct_count   INT DEFAULT 0,
    wrong_count     INT DEFAULT 0,
    is_learned      BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, word_id)
);

-- Сессии обучения
CREATE TABLE duolingo.learning_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES duolingo.users(id) ON DELETE CASCADE,
    language_id     INT NOT NULL REFERENCES duolingo.languages(id),
    started_at      TIMESTAMPTZ DEFAULT NOW(),
    finished_at     TIMESTAMPTZ,
    duration_sec    INT,
    words_studied   INT DEFAULT 0,
    correct_answers INT DEFAULT 0,
    wrong_answers   INT DEFAULT 0,
    xp_earned       INT DEFAULT 0
);

-- Результаты упражнений
CREATE TABLE duolingo.exercise_results (
    id              SERIAL PRIMARY KEY,
    session_id      UUID NOT NULL REFERENCES duolingo.learning_sessions(id) ON DELETE CASCADE,
    word_id         INT NOT NULL REFERENCES duolingo.words(id) ON DELETE CASCADE,
    exercise_type   duolingo.exercise_type DEFAULT 'translation',
    user_answer     TEXT,
    is_correct      BOOLEAN DEFAULT FALSE,
    response_time_ms INT,
    ai_feedback     TEXT,
    answered_at     TIMESTAMPTZ DEFAULT NOW()
);

-- Достижения
CREATE TABLE duolingo.achievements (
    id              SERIAL PRIMARY KEY,
    code            VARCHAR(100) NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    icon_emoji      VARCHAR(10),
    xp_reward       INT DEFAULT 0,
    condition_type  VARCHAR(50),
    condition_value INT
);

-- Достижения пользователя
CREATE TABLE duolingo.user_achievements (
    id              SERIAL PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES duolingo.users(id) ON DELETE CASCADE,
    achievement_id  INT NOT NULL REFERENCES duolingo.achievements(id) ON DELETE CASCADE,
    earned_at       TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, achievement_id)
);

-- ============================================================
-- ИНДЕКСЫ
-- ============================================================

CREATE INDEX idx_users_email ON duolingo.users(email);
CREATE INDEX idx_words_language ON duolingo.words(language_id);
CREATE INDEX idx_words_topic ON duolingo.words(topic_id);
CREATE INDEX idx_word_progresses_user ON duolingo.word_progresses(user_id);
CREATE INDEX idx_word_progresses_next_review ON duolingo.word_progresses(user_id, next_review);
CREATE INDEX idx_learning_sessions_user ON duolingo.learning_sessions(user_id);
CREATE INDEX idx_exercise_results_session ON duolingo.exercise_results(session_id);
CREATE INDEX idx_user_achievements_user ON duolingo.user_achievements(user_id);

-- ============================================================
-- RLS (Row Level Security) — базовые политики
-- ============================================================

ALTER TABLE duolingo.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.languages ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.topics ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.words ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.word_progresses ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.learning_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.exercise_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.achievements ENABLE ROW LEVEL SECURITY;
ALTER TABLE duolingo.user_achievements ENABLE ROW LEVEL SECURITY;

-- Разрешаем анонимный доступ ко всем таблицам (для простоты разработки)
-- В продакшене замените на более строгие политики

CREATE POLICY "allow_all_users" ON duolingo.users FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "allow_all_languages" ON duolingo.languages FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "allow_all_topics" ON duolingo.topics FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "allow_all_words" ON duolingo.words FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "allow_all_word_progresses" ON duolingo.word_progresses FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "allow_all_learning_sessions" ON duolingo.learning_sessions FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "allow_all_exercise_results" ON duolingo.exercise_results FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "allow_all_achievements" ON duolingo.achievements FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "allow_all_user_achievements" ON duolingo.user_achievements FOR ALL USING (true) WITH CHECK (true);

-- Добавляем схему в search_path для Supabase API
ALTER ROLE authenticator SET search_path TO duolingo, public;
ALTER ROLE anon SET search_path TO duolingo, public;
ALTER ROLE authenticated SET search_path TO duolingo, public;
ALTER ROLE service_role SET search_path TO duolingo, public;

-- Даём права на схему
GRANT USAGE ON SCHEMA duolingo TO anon, authenticated, service_role;
GRANT ALL ON ALL TABLES IN SCHEMA duolingo TO anon, authenticated, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA duolingo TO anon, authenticated, service_role;

-- ============================================================
-- НАЧАЛЬНЫЕ ДАННЫЕ
-- ============================================================

-- Языки
INSERT INTO duolingo.languages (code, name, native_name, flag_emoji, is_active) VALUES
    ('en', 'Английский', 'English', '🇬🇧', TRUE),
    ('ru', 'Русский', 'Русский', '🇷🇺', TRUE),
    ('de', 'Немецкий', 'Deutsch', '🇩🇪', TRUE),
    ('fr', 'Французский', 'Français', '🇫🇷', TRUE),
    ('es', 'Испанский', 'Español', '🇪🇸', TRUE),
    ('it', 'Итальянский', 'Italiano', '🇮🇹', TRUE),
    ('zh', 'Китайский', '中文', '🇨🇳', TRUE),
    ('ja', 'Японский', '日本語', '🇯🇵', TRUE),
    ('ko', 'Корейский', '한국어', '🇰🇷', TRUE),
    ('pt', 'Португальский', 'Português', '🇵🇹', TRUE);

-- Темы (для английского языка — id=1)
INSERT INTO duolingo.topics (language_id, name, description, icon_emoji, sort_order) VALUES
    (1, 'Приветствия', 'Основные фразы приветствия и прощания', '👋', 1),
    (1, 'Семья', 'Члены семьи и родственники', '👨‍👩‍👧‍👦', 2),
    (1, 'Еда и напитки', 'Продукты питания и напитки', '🍕', 3),
    (1, 'Животные', 'Домашние и дикие животные', '🐾', 4),
    (1, 'Цвета', 'Основные цвета', '🎨', 5),
    (1, 'Числа', 'Числа и счёт', '🔢', 6),
    (1, 'Путешествия', 'Транспорт и путешествия', '✈️', 7),
    (1, 'Работа', 'Профессии и офис', '💼', 8),
    (1, 'Дом', 'Предметы в доме', '🏠', 9),
    (1, 'Природа', 'Погода, растения, природа', '🌿', 10);

-- Слова (для английского языка)
INSERT INTO duolingo.words (language_id, topic_id, word, translation, transcription, example_sentence, example_translation) VALUES
    -- Приветствия (topic_id=1)
    (1, 1, 'hello', 'привет', 'həˈloʊ', 'Hello, how are you?', 'Привет, как дела?'),
    (1, 1, 'goodbye', 'до свидания', 'ɡʊdˈbaɪ', 'Goodbye, see you later!', 'До свидания, увидимся позже!'),
    (1, 1, 'good morning', 'доброе утро', 'ɡʊd ˈmɔːrnɪŋ', 'Good morning, everyone!', 'Доброе утро, всем!'),
    (1, 1, 'good night', 'спокойной ночи', 'ɡʊd naɪt', 'Good night, sweet dreams.', 'Спокойной ночи, сладких снов.'),
    (1, 1, 'thank you', 'спасибо', 'θæŋk juː', 'Thank you very much!', 'Спасибо большое!'),
    (1, 1, 'please', 'пожалуйста', 'pliːz', 'Please help me.', 'Пожалуйста, помогите мне.'),
    (1, 1, 'sorry', 'извините', 'ˈsɑːri', 'I am sorry for being late.', 'Извините за опоздание.'),
    (1, 1, 'yes', 'да', 'jes', 'Yes, I agree.', 'Да, я согласен.'),
    (1, 1, 'no', 'нет', 'noʊ', 'No, thank you.', 'Нет, спасибо.'),
    (1, 1, 'how are you', 'как дела', 'haʊ ɑːr juː', 'How are you today?', 'Как у тебя дела сегодня?'),

    -- Семья (topic_id=2)
    (1, 2, 'mother', 'мама', 'ˈmʌðər', 'My mother is kind.', 'Моя мама добрая.'),
    (1, 2, 'father', 'папа', 'ˈfɑːðər', 'My father works hard.', 'Мой папа много работает.'),
    (1, 2, 'brother', 'брат', 'ˈbrʌðər', 'I have one brother.', 'У меня есть один брат.'),
    (1, 2, 'sister', 'сестра', 'ˈsɪstər', 'My sister is older.', 'Моя сестра старше.'),
    (1, 2, 'family', 'семья', 'ˈfæməli', 'I love my family.', 'Я люблю свою семью.'),
    (1, 2, 'child', 'ребёнок', 'tʃaɪld', 'The child is playing.', 'Ребёнок играет.'),
    (1, 2, 'husband', 'муж', 'ˈhʌzbənd', 'Her husband is a doctor.', 'Её муж — врач.'),
    (1, 2, 'wife', 'жена', 'waɪf', 'His wife is a teacher.', 'Его жена — учительница.'),
    (1, 2, 'son', 'сын', 'sʌn', 'Their son is five years old.', 'Их сыну пять лет.'),
    (1, 2, 'daughter', 'дочь', 'ˈdɔːtər', 'My daughter likes to read.', 'Моя дочь любит читать.'),

    -- Еда и напитки (topic_id=3)
    (1, 3, 'water', 'вода', 'ˈwɔːtər', 'I drink water every day.', 'Я пью воду каждый день.'),
    (1, 3, 'bread', 'хлеб', 'bred', 'I buy fresh bread.', 'Я покупаю свежий хлеб.'),
    (1, 3, 'milk', 'молоко', 'mɪlk', 'The milk is cold.', 'Молоко холодное.'),
    (1, 3, 'apple', 'яблоко', 'ˈæpəl', 'I eat an apple for breakfast.', 'Я ем яблоко на завтрак.'),
    (1, 3, 'coffee', 'кофе', 'ˈkɔːfi', 'I need my morning coffee.', 'Мне нужен утренний кофе.'),
    (1, 3, 'tea', 'чай', 'tiː', 'Would you like some tea?', 'Хотите чаю?'),
    (1, 3, 'food', 'еда', 'fuːd', 'The food is delicious.', 'Еда вкусная.'),
    (1, 3, 'meat', 'мясо', 'miːt', 'I don''t eat meat.', 'Я не ем мясо.'),
    (1, 3, 'fish', 'рыба', 'fɪʃ', 'The fish is fresh.', 'Рыба свежая.'),
    (1, 3, 'rice', 'рис', 'raɪs', 'Rice is popular in Asia.', 'Рис популярен в Азии.'),

    -- Животные (topic_id=4)
    (1, 4, 'cat', 'кот', 'kæt', 'The cat is sleeping.', 'Кот спит.'),
    (1, 4, 'dog', 'собака', 'dɒɡ', 'The dog is running.', 'Собака бежит.'),
    (1, 4, 'bird', 'птица', 'bɜːrd', 'The bird is singing.', 'Птица поёт.'),
    (1, 4, 'horse', 'лошадь', 'hɔːrs', 'The horse runs fast.', 'Лошадь бежит быстро.'),
    (1, 4, 'fish', 'рыба', 'fɪʃ', 'The fish swims in the pond.', 'Рыба плавает в пруду.'),
    (1, 4, 'elephant', 'слон', 'ˈelɪfənt', 'The elephant is big.', 'Слон большой.'),
    (1, 4, 'lion', 'лев', 'ˈlaɪən', 'The lion is the king of animals.', 'Лев — царь зверей.'),
    (1, 4, 'bear', 'медведь', 'ber', 'The bear lives in the forest.', 'Медведь живёт в лесу.'),
    (1, 4, 'rabbit', 'кролик', 'ˈræbɪt', 'The rabbit is white.', 'Кролик белый.'),
    (1, 4, 'wolf', 'волк', 'wʊlf', 'The wolf howls at night.', 'Волк воет по ночам.'),

    -- Цвета (topic_id=5)
    (1, 5, 'red', 'красный', 'red', 'The car is red.', 'Машина красная.'),
    (1, 5, 'blue', 'синий', 'bluː', 'The sky is blue.', 'Небо синее.'),
    (1, 5, 'green', 'зелёный', 'ɡriːn', 'The grass is green.', 'Трава зелёная.'),
    (1, 5, 'yellow', 'жёлтый', 'ˈjeloʊ', 'The sun is yellow.', 'Солнце жёлтое.'),
    (1, 5, 'black', 'чёрный', 'blæk', 'The cat is black.', 'Кот чёрный.'),
    (1, 5, 'white', 'белый', 'waɪt', 'Snow is white.', 'Снег белый.'),
    (1, 5, 'orange', 'оранжевый', 'ˈɔːrɪndʒ', 'The orange is orange.', 'Апельсин оранжевый.'),
    (1, 5, 'purple', 'фиолетовый', 'ˈpɜːrpəl', 'The flower is purple.', 'Цветок фиолетовый.'),
    (1, 5, 'pink', 'розовый', 'pɪŋk', 'Her dress is pink.', 'Её платье розовое.'),
    (1, 5, 'brown', 'коричневый', 'braʊn', 'The table is brown.', 'Стол коричневый.');

-- Достижения
INSERT INTO duolingo.achievements (code, name, description, icon_emoji, xp_reward, condition_type, condition_value) VALUES
    ('first_word',      'Первое слово',         'Выучите первое слово',                     '🌱', 10,  'words_learned', 1),
    ('ten_words',       'Десяток',              'Выучите 10 слов',                          '📚', 25,  'words_learned', 10),
    ('fifty_words',     'Полтинник',            'Выучите 50 слов',                          '🎓', 50,  'words_learned', 50),
    ('hundred_words',   'Сотня',               'Выучите 100 слов',                         '💯', 100, 'words_learned', 100),
    ('first_streak',    'Начало пути',          'Занимайтесь 3 дня подряд',                 '🔥', 15,  'streak_days', 3),
    ('week_streak',     'Недельная серия',       'Занимайтесь 7 дней подряд',                '⚡', 30,  'streak_days', 7),
    ('month_streak',    'Месячная серия',        'Занимайтесь 30 дней подряд',               '🏆', 100, 'streak_days', 30),
    ('first_session',   'Первый урок',          'Завершите первый урок',                    '🎯', 10,  'sessions_count', 1),
    ('ten_sessions',    'Десять уроков',         'Завершите 10 уроков',                     '📖', 30,  'sessions_count', 10),
    ('fifty_sessions',  'Пятьдесят уроков',     'Завершите 50 уроков',                     '🌟', 75,  'sessions_count', 50),
    ('hundred_correct', 'Сотня правильных',     'Дайте 100 правильных ответов',             '✅', 50,  'correct_answers', 100),
    ('five_hundred_correct', 'Пятьсот правильных', 'Дайте 500 правильных ответов',          '🏅', 100, 'correct_answers', 500),
    ('perfect_session', 'Идеальный урок',       'Завершите урок без ошибок',                '💎', 25,  'perfect_sessions', 1),
    ('five_perfect',    'Перфекционист',         'Завершите 5 уроков без ошибок',            '👑', 75,  'perfect_sessions', 5),
    ('xp_100',          'Новичок',              'Наберите 100 XP',                          '⭐', 20,  'total_xp', 100),
    ('xp_500',          'Опытный',              'Наберите 500 XP',                          '🌠', 50,  'total_xp', 500),
    ('xp_1000',         'Мастер',               'Наберите 1000 XP',                         '🎖️', 100, 'total_xp', 1000);

-- ============================================================
-- Готово! Схема создана и наполнена начальными данными.
-- Теперь замените SUPABASE_URL и SUPABASE_ANON_KEY в app/build.gradle.kts
-- ============================================================
