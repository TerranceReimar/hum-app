import os
import sqlite3
from datetime import date, timedelta

DB_PATH = os.environ.get("DB_PATH", "gymtracker.db")


def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    try:
        yield conn
    finally:
        conn.close()


def init_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    c = conn.cursor()

    c.executescript("""
        -- ── Profiles ──────────────────────────────────────────────────────────

        CREATE TABLE IF NOT EXISTS profiles (
            id                  INTEGER PRIMARY KEY AUTOINCREMENT,
            name                TEXT NOT NULL UNIQUE,
            password            TEXT NOT NULL DEFAULT '',
            notification_time   TEXT NOT NULL DEFAULT '20:00',
            goal_weight         REAL,
            goal_waist          REAL,
            goal_arm            REAL,
            goal_chest          REAL,
            goal_thigh          REAL,
            goal_neck           REAL,
            goal_hip            REAL,
            created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- ── Exercise library ──────────────────────────────────────────────────

        CREATE TABLE IF NOT EXISTS exercise_list (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            name        TEXT NOT NULL UNIQUE,
            category    TEXT NOT NULL DEFAULT 'Other'
        );

        -- ── Measurement types + weekly metrics ────────────────────────────────

        CREATE TABLE IF NOT EXISTS measurement_types (
            id      INTEGER PRIMARY KEY AUTOINCREMENT,
            name    TEXT NOT NULL UNIQUE
        );

        CREATE TABLE IF NOT EXISTS user_metrics (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            profile_id  INTEGER REFERENCES profiles(id) ON DELETE CASCADE,
            timestamp   TEXT NOT NULL,
            metric_type TEXT NOT NULL,
            value       REAL NOT NULL,
            notes       TEXT DEFAULT ''
        );

        -- ── Workout schedule ──────────────────────────────────────────────────

        CREATE TABLE IF NOT EXISTS workout_blocks (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            profile_id      INTEGER NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
            exercise_name   TEXT NOT NULL,
            sets            INTEGER NOT NULL,
            reps            INTEGER NOT NULL,
            notes           TEXT DEFAULT ''
        );

        CREATE TABLE IF NOT EXISTS workout_days (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            profile_id  INTEGER NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
            name        TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS workout_day_blocks (
            id                  INTEGER PRIMARY KEY AUTOINCREMENT,
            workout_day_id      INTEGER NOT NULL REFERENCES workout_days(id) ON DELETE CASCADE,
            workout_block_id    INTEGER NOT NULL REFERENCES workout_blocks(id) ON DELETE CASCADE,
            sort_order          INTEGER NOT NULL DEFAULT 0
        );

        -- ── Diet schedule ─────────────────────────────────────────────────────

        CREATE TABLE IF NOT EXISTS meal_blocks (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            profile_id  INTEGER NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
            name        TEXT NOT NULL,
            description TEXT DEFAULT '',
            calories    INTEGER DEFAULT 0,
            protein_g   REAL DEFAULT 0,
            carbs_g     REAL DEFAULT 0,
            fat_g       REAL DEFAULT 0
        );

        CREATE TABLE IF NOT EXISTS diet_days (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            profile_id  INTEGER NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
            name        TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS diet_day_meals (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            diet_day_id     INTEGER NOT NULL REFERENCES diet_days(id) ON DELETE CASCADE,
            meal_block_id   INTEGER NOT NULL REFERENCES meal_blocks(id) ON DELETE CASCADE,
            sort_order      INTEGER NOT NULL DEFAULT 0
        );

        -- ── Weekly schedule ───────────────────────────────────────────────────
        -- day_of_week: 0=Monday … 6=Sunday

        CREATE TABLE IF NOT EXISTS schedule (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            profile_id      INTEGER NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
            day_of_week     INTEGER NOT NULL CHECK(day_of_week BETWEEN 0 AND 6),
            workout_day_id  INTEGER REFERENCES workout_days(id) ON DELETE SET NULL,
            diet_day_id     INTEGER REFERENCES diet_days(id) ON DELETE SET NULL,
            UNIQUE(profile_id, day_of_week)
        );

        -- ── Daily confirmation ────────────────────────────────────────────────

        CREATE TABLE IF NOT EXISTS daily_logs (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            profile_id  INTEGER NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
            date        TEXT NOT NULL,
            score       REAL NOT NULL DEFAULT 0.0,
            note        TEXT DEFAULT '',
            created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE(profile_id, date)
        );

        -- block_type: 'workout' | 'diet'
        -- block_id references workout_blocks.id or meal_blocks.id
        -- completed + floor_only:
        --   completed=1, floor_only=0  → full block done        (score: 1.0)
        --   completed=1, floor_only=1  → floor version done      (score: 0.5)
        --   completed=0               → skipped                  (score: 0.0)

        CREATE TABLE IF NOT EXISTS daily_log_blocks (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            daily_log_id    INTEGER NOT NULL REFERENCES daily_logs(id) ON DELETE CASCADE,
            block_type      TEXT NOT NULL CHECK(block_type IN ('workout', 'diet')),
            block_id        INTEGER NOT NULL,
            completed       INTEGER NOT NULL DEFAULT 0,
            floor_only      INTEGER NOT NULL DEFAULT 0
        );

        -- ── Sprints ───────────────────────────────────────────────────────────

        CREATE TABLE IF NOT EXISTS sprints (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            start_date  TEXT NOT NULL,
            end_date    TEXT NOT NULL
        );

        -- ── Default seed data ─────────────────────────────────────────────────

        INSERT OR IGNORE INTO exercise_list (name, category) VALUES
            ('Bench Press',             'Push'),
            ('Pull-Ups',                'Pull'),
            ('Squat',                   'Legs'),
            ('Deadlift',                'Pull'),
            ('Overhead Press',          'Push'),
            ('Weighted Ring Dips',      'Push'),
            ('Bulgarian Split Squats',  'Legs'),
            ('Barbell Row',             'Pull'),
            ('Romanian Deadlift',       'Legs'),
            ('Incline Dumbbell Press',  'Push');

        INSERT OR IGNORE INTO measurement_types (name) VALUES
            ('Weight'),
            ('Waist (Navel)'),
            ('Arm (Flexed)'),
            ('Chest'),
            ('Thigh'),
            ('Neck'),
            ('Hip');
    """)
    conn.commit()

    # ── Migrations: add columns to pre-existing databases ────────────────────
    for sql in [
        "ALTER TABLE profiles ADD COLUMN notification_time TEXT NOT NULL DEFAULT '20:00'",
        "ALTER TABLE profiles ADD COLUMN goal_weight REAL",
        "ALTER TABLE profiles ADD COLUMN goal_waist  REAL",
        "ALTER TABLE profiles ADD COLUMN goal_arm    REAL",
        "ALTER TABLE profiles ADD COLUMN goal_chest  REAL",
        "ALTER TABLE profiles ADD COLUMN goal_thigh  REAL",
        "ALTER TABLE profiles ADD COLUMN goal_neck   REAL",
        "ALTER TABLE profiles ADD COLUMN goal_hip    REAL",
    ]:
        try:
            conn.execute(sql)
            conn.commit()
        except Exception:
            pass

    # ── Ensure an active sprint exists ────────────────────────────────────────
    _ensure_current_sprint(conn)
    conn.close()


def _ensure_current_sprint(conn):
    today = date.today()
    active = conn.execute(
        "SELECT id FROM sprints WHERE start_date <= ? AND end_date >= ?",
        (today.isoformat(), today.isoformat()),
    ).fetchone()
    if active:
        return

    last = conn.execute(
        "SELECT * FROM sprints ORDER BY end_date DESC LIMIT 1"
    ).fetchone()

    if last:
        last_end = date.fromisoformat(last["end_date"])
        # Next sprint starts the Monday immediately after the last one ends
        days_to_monday = (7 - last_end.weekday()) % 7
        if days_to_monday == 0:
            days_to_monday = 7
        start = last_end + timedelta(days=days_to_monday)
    else:
        # First sprint: begin from the most recent Monday (today if Monday)
        start = today - timedelta(days=today.weekday())

    end = start + timedelta(days=89)  # 90 days inclusive
    conn.execute(
        "INSERT INTO sprints (start_date, end_date) VALUES (?, ?)",
        (start.isoformat(), end.isoformat()),
    )
    conn.commit()


def get_current_sprint(conn) -> dict | None:
    """Return the active sprint, creating it if needed."""
    _ensure_current_sprint(conn)
    today = date.today()
    row = conn.execute(
        "SELECT * FROM sprints WHERE start_date <= ? AND end_date >= ?",
        (today.isoformat(), today.isoformat()),
    ).fetchone()
    return dict(row) if row else None
