import sqlite3
from fastapi import APIRouter, Depends, HTTPException
from database import get_db
from models import WorkoutBlockCreate, WorkoutBlockUpdate, WorkoutDayCreate, WorkoutDayBlockAdd, ReorderItems

router = APIRouter(prefix="/profiles/{profile_id}", tags=["workout-schedule"])


# ── Helpers ───────────────────────────────────────────────────────────────────

def _day_with_blocks(day_id: int, db) -> dict | None:
    day = db.execute("SELECT * FROM workout_days WHERE id=?", (day_id,)).fetchone()
    if not day:
        return None
    blocks = db.execute(
        """SELECT wb.*, wdb.id AS entry_id, wdb.sort_order
           FROM workout_day_blocks wdb
           JOIN workout_blocks wb ON wb.id = wdb.workout_block_id
           WHERE wdb.workout_day_id = ?
           ORDER BY wdb.sort_order""",
        (day_id,),
    ).fetchall()
    return {**dict(day), "blocks": [dict(b) for b in blocks]}


# ── Workout blocks ────────────────────────────────────────────────────────────

@router.get("/workout-blocks")
def list_workout_blocks(profile_id: int, db: sqlite3.Connection = Depends(get_db)):
    rows = db.execute(
        "SELECT * FROM workout_blocks WHERE profile_id=? ORDER BY exercise_name",
        (profile_id,),
    ).fetchall()
    return [dict(r) for r in rows]


@router.post("/workout-blocks", status_code=201)
def create_workout_block(
    profile_id: int,
    body: WorkoutBlockCreate,
    db: sqlite3.Connection = Depends(get_db),
):
    cur = db.execute(
        "INSERT INTO workout_blocks (profile_id, exercise_name, sets, reps, notes) VALUES (?,?,?,?,?)",
        (profile_id, body.exercise_name, body.sets, body.reps, body.notes),
    )
    db.commit()
    return dict(db.execute("SELECT * FROM workout_blocks WHERE id=?", (cur.lastrowid,)).fetchone())


@router.put("/workout-blocks/{block_id}")
def update_workout_block(
    profile_id: int,
    block_id: int,
    body: WorkoutBlockUpdate,
    db: sqlite3.Connection = Depends(get_db),
):
    row = db.execute(
        "SELECT * FROM workout_blocks WHERE id=? AND profile_id=?", (block_id, profile_id)
    ).fetchone()
    if not row:
        raise HTTPException(404, "Block not found")
    cur = dict(row)
    db.execute(
        "UPDATE workout_blocks SET exercise_name=?, sets=?, reps=?, notes=? WHERE id=?",
        (
            body.exercise_name if body.exercise_name is not None else cur["exercise_name"],
            body.sets          if body.sets          is not None else cur["sets"],
            body.reps          if body.reps          is not None else cur["reps"],
            body.notes         if body.notes         is not None else cur["notes"],
            block_id,
        ),
    )
    db.commit()
    return dict(db.execute("SELECT * FROM workout_blocks WHERE id=?", (block_id,)).fetchone())


@router.delete("/workout-blocks/{block_id}", status_code=204)
def delete_workout_block(
    profile_id: int,
    block_id: int,
    db: sqlite3.Connection = Depends(get_db),
):
    db.execute(
        "DELETE FROM workout_blocks WHERE id=? AND profile_id=?", (block_id, profile_id)
    )
    db.commit()


# ── Workout days ──────────────────────────────────────────────────────────────

@router.get("/workout-days")
def list_workout_days(profile_id: int, db: sqlite3.Connection = Depends(get_db)):
    days = db.execute(
        "SELECT * FROM workout_days WHERE profile_id=? ORDER BY name", (profile_id,)
    ).fetchall()
    return [_day_with_blocks(d["id"], db) for d in days]


@router.post("/workout-days", status_code=201)
def create_workout_day(
    profile_id: int,
    body: WorkoutDayCreate,
    db: sqlite3.Connection = Depends(get_db),
):
    cur = db.execute(
        "INSERT INTO workout_days (profile_id, name) VALUES (?,?)", (profile_id, body.name)
    )
    db.commit()
    return _day_with_blocks(cur.lastrowid, db)


@router.put("/workout-days/{day_id}")
def update_workout_day(
    profile_id: int,
    day_id: int,
    body: WorkoutDayCreate,
    db: sqlite3.Connection = Depends(get_db),
):
    row = db.execute(
        "SELECT id FROM workout_days WHERE id=? AND profile_id=?", (day_id, profile_id)
    ).fetchone()
    if not row:
        raise HTTPException(404, "Workout day not found")
    db.execute("UPDATE workout_days SET name=? WHERE id=?", (body.name, day_id))
    db.commit()
    return _day_with_blocks(day_id, db)


@router.delete("/workout-days/{day_id}", status_code=204)
def delete_workout_day(
    profile_id: int,
    day_id: int,
    db: sqlite3.Connection = Depends(get_db),
):
    db.execute(
        "DELETE FROM workout_days WHERE id=? AND profile_id=?", (day_id, profile_id)
    )
    db.commit()


# ── Blocks within a day ───────────────────────────────────────────────────────

@router.post("/workout-days/{day_id}/blocks", status_code=201)
def add_block_to_day(
    profile_id: int,
    day_id: int,
    body: WorkoutDayBlockAdd,
    db: sqlite3.Connection = Depends(get_db),
):
    if not db.execute(
        "SELECT id FROM workout_days WHERE id=? AND profile_id=?", (day_id, profile_id)
    ).fetchone():
        raise HTTPException(404, "Workout day not found")
    if not db.execute(
        "SELECT id FROM workout_blocks WHERE id=? AND profile_id=?",
        (body.workout_block_id, profile_id),
    ).fetchone():
        raise HTTPException(404, "Workout block not found")
    db.execute(
        "INSERT INTO workout_day_blocks (workout_day_id, workout_block_id, sort_order) VALUES (?,?,?)",
        (day_id, body.workout_block_id, body.sort_order),
    )
    db.commit()
    return _day_with_blocks(day_id, db)


@router.delete("/workout-days/{day_id}/blocks/{entry_id}", status_code=204)
def remove_block_from_day(
    profile_id: int,
    day_id: int,
    entry_id: int,
    db: sqlite3.Connection = Depends(get_db),
):
    db.execute(
        "DELETE FROM workout_day_blocks WHERE id=? AND workout_day_id=?",
        (entry_id, day_id),
    )
    db.commit()


@router.put("/workout-days/{day_id}/blocks/reorder")
def reorder_day_blocks(
    profile_id: int,
    day_id: int,
    body: ReorderItems,
    db: sqlite3.Connection = Depends(get_db),
):
    for i, entry_id in enumerate(body.ordered_ids):
        db.execute(
            "UPDATE workout_day_blocks SET sort_order=? WHERE id=? AND workout_day_id=?",
            (i, entry_id, day_id),
        )
    db.commit()
    return _day_with_blocks(day_id, db)
