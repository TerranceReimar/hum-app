from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List
import sqlite3
import os
from datetime import datetime

app = FastAPI(title="Hum API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

DB_PATH = os.environ.get("DB_PATH", "gymtracker.db")


def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
    finally:
        conn.close()


def init_db():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.executescript("""
        CREATE TABLE IF NOT EXISTS exercise_list (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE,
            category TEXT NOT NULL DEFAULT 'Other'
        );

        CREATE TABLE IF NOT EXISTS profiles (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE,
            starting_weight REAL,
            current_weight REAL,
            goal_weight REAL,
            starting_waist REAL,
            starting_arm REAL,
            starting_chest REAL,
            starting_thigh REAL,
            starting_neck REAL,
            starting_hip REAL,
            current_waist REAL,
            current_arm REAL,
            current_chest REAL,
            current_thigh REAL,
            current_neck REAL,
            current_hip REAL,
            goal_waist REAL,
            goal_arm REAL,
            goal_chest REAL,
            goal_thigh REAL,
            goal_neck REAL,
            goal_hip REAL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS workouts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp TEXT NOT NULL,
            exercise_name TEXT NOT NULL,
            sets INTEGER NOT NULL,
            reps INTEGER NOT NULL,
            weight REAL NOT NULL,
            tempo TEXT DEFAULT ''
        );

        CREATE TABLE IF NOT EXISTS user_metrics (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp TEXT NOT NULL,
            metric_type TEXT NOT NULL,
            value REAL NOT NULL,
            notes TEXT DEFAULT ''
        );

        CREATE TABLE IF NOT EXISTS measurement_types (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE
        );

        INSERT OR IGNORE INTO exercise_list (name, category) VALUES
            ('Bench Press', 'Push'),
            ('Pull-Ups', 'Pull'),
            ('Squat', 'Legs'),
            ('Deadlift', 'Pull'),
            ('Overhead Press', 'Push'),
            ('Weighted Ring Dips', 'Push'),
            ('Bulgarian Split Squats', 'Legs'),
            ('Barbell Row', 'Pull'),
            ('Romanian Deadlift', 'Legs'),
            ('Incline Dumbbell Press', 'Push');

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

    # Migrations: add profile_id to existing tables without breaking data
    for sql in [
        "ALTER TABLE workouts ADD COLUMN profile_id INTEGER REFERENCES profiles(id)",
        "ALTER TABLE user_metrics ADD COLUMN profile_id INTEGER REFERENCES profiles(id)",
    ]:
        try:
            conn.execute(sql)
            conn.commit()
        except Exception:
            pass

    conn.close()


# ── Models ────────────────────────────────────────────────────────────────────

class ExerciseCreate(BaseModel):
    name: str
    category: str = "Other"

class ExerciseUpdate(BaseModel):
    name: Optional[str] = None
    category: Optional[str] = None

class WorkoutCreate(BaseModel):
    exercise_name: str
    sets: int
    reps: int
    weight: float
    tempo: str = ""
    timestamp: Optional[str] = None
    profile_id: Optional[int] = None

class MetricCreate(BaseModel):
    metric_type: str
    value: float
    notes: str = ""
    timestamp: Optional[str] = None
    profile_id: Optional[int] = None

class MeasurementTypeCreate(BaseModel):
    name: str

class ProfileCreate(BaseModel):
    name: str
    starting_weight: Optional[float] = None
    current_weight: Optional[float] = None
    goal_weight: Optional[float] = None
    starting_waist: Optional[float] = None
    starting_arm: Optional[float] = None
    starting_chest: Optional[float] = None
    starting_thigh: Optional[float] = None
    starting_neck: Optional[float] = None
    starting_hip: Optional[float] = None
    current_waist: Optional[float] = None
    current_arm: Optional[float] = None
    current_chest: Optional[float] = None
    current_thigh: Optional[float] = None
    current_neck: Optional[float] = None
    current_hip: Optional[float] = None
    goal_waist: Optional[float] = None
    goal_arm: Optional[float] = None
    goal_chest: Optional[float] = None
    goal_thigh: Optional[float] = None
    goal_neck: Optional[float] = None
    goal_hip: Optional[float] = None

class ProfileUpdate(BaseModel):
    name: Optional[str] = None
    starting_weight: Optional[float] = None
    current_weight: Optional[float] = None
    goal_weight: Optional[float] = None
    starting_waist: Optional[float] = None
    starting_arm: Optional[float] = None
    starting_chest: Optional[float] = None
    starting_thigh: Optional[float] = None
    starting_neck: Optional[float] = None
    starting_hip: Optional[float] = None
    current_waist: Optional[float] = None
    current_arm: Optional[float] = None
    current_chest: Optional[float] = None
    current_thigh: Optional[float] = None
    current_neck: Optional[float] = None
    current_hip: Optional[float] = None
    goal_waist: Optional[float] = None
    goal_arm: Optional[float] = None
    goal_chest: Optional[float] = None
    goal_thigh: Optional[float] = None
    goal_neck: Optional[float] = None
    goal_hip: Optional[float] = None


# ── Profiles ──────────────────────────────────────────────────────────────────

@app.get("/profiles", response_model=List[dict])
def list_profiles(db: sqlite3.Connection = Depends(get_db)):
    rows = db.execute("SELECT * FROM profiles ORDER BY name").fetchall()
    return [dict(r) for r in rows]

@app.post("/profiles", status_code=201)
def create_profile(body: ProfileCreate, db: sqlite3.Connection = Depends(get_db)):
    try:
        cur = db.execute(
            """INSERT INTO profiles (
                name, starting_weight, current_weight, goal_weight,
                starting_waist, starting_arm, starting_chest, starting_thigh, starting_neck, starting_hip,
                current_waist, current_arm, current_chest, current_thigh, current_neck, current_hip,
                goal_waist, goal_arm, goal_chest, goal_thigh, goal_neck, goal_hip
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            (
                body.name, body.starting_weight, body.current_weight, body.goal_weight,
                body.starting_waist, body.starting_arm, body.starting_chest, body.starting_thigh, body.starting_neck, body.starting_hip,
                body.current_waist, body.current_arm, body.current_chest, body.current_thigh, body.current_neck, body.current_hip,
                body.goal_waist, body.goal_arm, body.goal_chest, body.goal_thigh, body.goal_neck, body.goal_hip,
            )
        )
        db.commit()
        profile = db.execute("SELECT * FROM profiles WHERE id=?", (cur.lastrowid,)).fetchone()
        return dict(profile)
    except sqlite3.IntegrityError:
        raise HTTPException(400, "Profile name already exists")

@app.get("/profiles/{profile_id}", response_model=dict)
def get_profile(profile_id: int, db: sqlite3.Connection = Depends(get_db)):
    row = db.execute("SELECT * FROM profiles WHERE id=?", (profile_id,)).fetchone()
    if not row:
        raise HTTPException(404, "Profile not found")
    return dict(row)

@app.put("/profiles/{profile_id}")
def update_profile(profile_id: int, body: ProfileUpdate, db: sqlite3.Connection = Depends(get_db)):
    row = db.execute("SELECT * FROM profiles WHERE id=?", (profile_id,)).fetchone()
    if not row:
        raise HTTPException(404, "Profile not found")
    current = dict(row)
    fields = [
        "name", "starting_weight", "current_weight", "goal_weight",
        "starting_waist", "starting_arm", "starting_chest", "starting_thigh", "starting_neck", "starting_hip",
        "current_waist", "current_arm", "current_chest", "current_thigh", "current_neck", "current_hip",
        "goal_waist", "goal_arm", "goal_chest", "goal_thigh", "goal_neck", "goal_hip",
    ]
    updates = {f: getattr(body, f) if getattr(body, f) is not None else current[f] for f in fields}
    db.execute(
        f"UPDATE profiles SET {', '.join(f + '=?' for f in fields)} WHERE id=?",
        [*updates.values(), profile_id]
    )
    db.commit()
    updated = db.execute("SELECT * FROM profiles WHERE id=?", (profile_id,)).fetchone()
    return dict(updated)

@app.delete("/profiles/{profile_id}", status_code=204)
def delete_profile(profile_id: int, db: sqlite3.Connection = Depends(get_db)):
    db.execute("DELETE FROM profiles WHERE id=?", (profile_id,))
    db.commit()


# ── Exercises ─────────────────────────────────────────────────────────────────

@app.get("/exercises", response_model=List[dict])
def list_exercises(db: sqlite3.Connection = Depends(get_db)):
    rows = db.execute("SELECT * FROM exercise_list ORDER BY category, name").fetchall()
    return [dict(r) for r in rows]

@app.post("/exercises", status_code=201)
def create_exercise(body: ExerciseCreate, db: sqlite3.Connection = Depends(get_db)):
    try:
        cur = db.execute(
            "INSERT INTO exercise_list (name, category) VALUES (?, ?)",
            (body.name, body.category)
        )
        db.commit()
        return {"id": cur.lastrowid, "name": body.name, "category": body.category}
    except sqlite3.IntegrityError:
        raise HTTPException(400, "Exercise already exists")

@app.put("/exercises/{exercise_id}")
def update_exercise(exercise_id: int, body: ExerciseUpdate, db: sqlite3.Connection = Depends(get_db)):
    ex = db.execute("SELECT * FROM exercise_list WHERE id = ?", (exercise_id,)).fetchone()
    if not ex:
        raise HTTPException(404, "Not found")
    name = body.name or ex["name"]
    category = body.category or ex["category"]
    db.execute("UPDATE exercise_list SET name=?, category=? WHERE id=?", (name, category, exercise_id))
    db.commit()
    return {"id": exercise_id, "name": name, "category": category}

@app.delete("/exercises/{exercise_id}", status_code=204)
def delete_exercise(exercise_id: int, db: sqlite3.Connection = Depends(get_db)):
    db.execute("DELETE FROM exercise_list WHERE id = ?", (exercise_id,))
    db.commit()


# ── Measurement Types ─────────────────────────────────────────────────────────

@app.get("/measurement-types", response_model=List[dict])
def list_measurement_types(db: sqlite3.Connection = Depends(get_db)):
    rows = db.execute("SELECT * FROM measurement_types ORDER BY name").fetchall()
    return [dict(r) for r in rows]

@app.post("/measurement-types", status_code=201)
def create_measurement_type(body: MeasurementTypeCreate, db: sqlite3.Connection = Depends(get_db)):
    try:
        cur = db.execute("INSERT INTO measurement_types (name) VALUES (?)", (body.name,))
        db.commit()
        return {"id": cur.lastrowid, "name": body.name}
    except sqlite3.IntegrityError:
        raise HTTPException(400, "Already exists")

@app.delete("/measurement-types/{type_id}", status_code=204)
def delete_measurement_type(type_id: int, db: sqlite3.Connection = Depends(get_db)):
    db.execute("DELETE FROM measurement_types WHERE id = ?", (type_id,))
    db.commit()


# ── Workouts ──────────────────────────────────────────────────────────────────

@app.get("/workouts", response_model=List[dict])
def list_workouts(limit: int = 50, profile_id: Optional[int] = None, db: sqlite3.Connection = Depends(get_db)):
    if profile_id is not None:
        rows = db.execute(
            "SELECT * FROM workouts WHERE profile_id=? ORDER BY timestamp DESC LIMIT ?",
            (profile_id, limit)
        ).fetchall()
    else:
        rows = db.execute(
            "SELECT * FROM workouts ORDER BY timestamp DESC LIMIT ?", (limit,)
        ).fetchall()
    return [dict(r) for r in rows]

@app.post("/workouts", status_code=201)
def log_workout(body: WorkoutCreate, db: sqlite3.Connection = Depends(get_db)):
    ts = body.timestamp or datetime.utcnow().isoformat()
    cur = db.execute(
        "INSERT INTO workouts (timestamp, exercise_name, sets, reps, weight, tempo, profile_id) VALUES (?,?,?,?,?,?,?)",
        (ts, body.exercise_name, body.sets, body.reps, body.weight, body.tempo, body.profile_id)
    )
    db.commit()
    return {"id": cur.lastrowid, "timestamp": ts}

@app.delete("/workouts/{workout_id}", status_code=204)
def delete_workout(workout_id: int, db: sqlite3.Connection = Depends(get_db)):
    db.execute("DELETE FROM workouts WHERE id = ?", (workout_id,))
    db.commit()


# ── Metrics ───────────────────────────────────────────────────────────────────

@app.get("/metrics", response_model=List[dict])
def list_metrics(limit: int = 100, metric_type: Optional[str] = None, profile_id: Optional[int] = None, db: sqlite3.Connection = Depends(get_db)):
    if metric_type and profile_id is not None:
        rows = db.execute(
            "SELECT * FROM user_metrics WHERE metric_type=? AND profile_id=? ORDER BY timestamp DESC LIMIT ?",
            (metric_type, profile_id, limit)
        ).fetchall()
    elif metric_type:
        rows = db.execute(
            "SELECT * FROM user_metrics WHERE metric_type=? ORDER BY timestamp DESC LIMIT ?",
            (metric_type, limit)
        ).fetchall()
    elif profile_id is not None:
        rows = db.execute(
            "SELECT * FROM user_metrics WHERE profile_id=? ORDER BY timestamp DESC LIMIT ?",
            (profile_id, limit)
        ).fetchall()
    else:
        rows = db.execute(
            "SELECT * FROM user_metrics ORDER BY timestamp DESC LIMIT ?", (limit,)
        ).fetchall()
    return [dict(r) for r in rows]

@app.post("/metrics", status_code=201)
def log_metric(body: MetricCreate, db: sqlite3.Connection = Depends(get_db)):
    ts = body.timestamp or datetime.utcnow().isoformat()
    cur = db.execute(
        "INSERT INTO user_metrics (timestamp, metric_type, value, notes, profile_id) VALUES (?,?,?,?,?)",
        (ts, body.metric_type, body.value, body.notes, body.profile_id)
    )
    db.commit()
    return {"id": cur.lastrowid, "timestamp": ts}

@app.delete("/metrics/{metric_id}", status_code=204)
def delete_metric(metric_id: int, db: sqlite3.Connection = Depends(get_db)):
    db.execute("DELETE FROM user_metrics WHERE id = ?", (metric_id,))
    db.commit()


# ── Health ────────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "ok", "timestamp": datetime.utcnow().isoformat()}


if __name__ == "__main__":
    import uvicorn
    init_db()
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=False)
