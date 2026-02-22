import sqlite3
from fastapi import APIRouter, Depends, HTTPException
from database import get_db
from models import ExerciseCreate, ExerciseUpdate

router = APIRouter(prefix="/exercises", tags=["exercises"])


@router.get("")
def list_exercises(db: sqlite3.Connection = Depends(get_db)):
    rows = db.execute(
        "SELECT * FROM exercise_list ORDER BY category, name"
    ).fetchall()
    return [dict(r) for r in rows]


@router.post("", status_code=201)
def create_exercise(body: ExerciseCreate, db: sqlite3.Connection = Depends(get_db)):
    try:
        cur = db.execute(
            "INSERT INTO exercise_list (name, category) VALUES (?, ?)",
            (body.name, body.category),
        )
        db.commit()
        return {"id": cur.lastrowid, "name": body.name, "category": body.category}
    except sqlite3.IntegrityError:
        raise HTTPException(400, "Exercise already exists")


@router.put("/{exercise_id}")
def update_exercise(
    exercise_id: int,
    body: ExerciseUpdate,
    db: sqlite3.Connection = Depends(get_db),
):
    ex = db.execute(
        "SELECT * FROM exercise_list WHERE id=?", (exercise_id,)
    ).fetchone()
    if not ex:
        raise HTTPException(404, "Not found")
    name     = body.name     or ex["name"]
    category = body.category or ex["category"]
    db.execute(
        "UPDATE exercise_list SET name=?, category=? WHERE id=?",
        (name, category, exercise_id),
    )
    db.commit()
    return {"id": exercise_id, "name": name, "category": category}


@router.delete("/{exercise_id}", status_code=204)
def delete_exercise(exercise_id: int, db: sqlite3.Connection = Depends(get_db)):
    db.execute("DELETE FROM exercise_list WHERE id=?", (exercise_id,))
    db.commit()
