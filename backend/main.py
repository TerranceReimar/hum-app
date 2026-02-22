from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List
import sqlite3
import os
from datetime import datetime

app = FastAPI(title="GymTracker API", version="1.0.0")

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

class MetricCreate(BaseModel):
    metric_type: str
    value: float
    notes: str = ""
    timestamp: Optional[str] = None

class MeasurementTypeCreate(BaseModel):
    name: str


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
def list_workouts(limit: int = 50, db: sqlite3.Connection = Depends(get_db)):
    rows = db.execute(
        "SELECT * FROM workouts ORDER BY timestamp DESC LIMIT ?", (limit,)
    ).fetchall()
    return [dict(r) for r in rows]

@app.post("/workouts", status_code=201)
def log_workout(body: WorkoutCreate, db: sqlite3.Connection = Depends(get_db)):
    ts = body.timestamp or datetime.utcnow().isoformat()
    cur = db.execute(
        "INSERT INTO workouts (timestamp, exercise_name, sets, reps, weight, tempo) VALUES (?,?,?,?,?,?)",
        (ts, body.exercise_name, body.sets, body.reps, body.weight, body.tempo)
    )
    db.commit()
    return {"id": cur.lastrowid, "timestamp": ts}

@app.delete("/workouts/{workout_id}", status_code=204)
def delete_workout(workout_id: int, db: sqlite3.Connection = Depends(get_db)):
    db.execute("DELETE FROM workouts WHERE id = ?", (workout_id,))
    db.commit()


# ── Metrics ───────────────────────────────────────────────────────────────────

@app.get("/metrics", response_model=List[dict])
def list_metrics(limit: int = 100, metric_type: Optional[str] = None, db: sqlite3.Connection = Depends(get_db)):
    if metric_type:
        rows = db.execute(
            "SELECT * FROM user_metrics WHERE metric_type=? ORDER BY timestamp DESC LIMIT ?",
            (metric_type, limit)
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
        "INSERT INTO user_metrics (timestamp, metric_type, value, notes) VALUES (?,?,?,?)",
        (ts, body.metric_type, body.value, body.notes)
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
