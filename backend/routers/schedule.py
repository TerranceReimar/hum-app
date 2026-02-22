import sqlite3
from fastapi import APIRouter, Depends, HTTPException
from database import get_db
from models import ScheduleDayUpdate

router = APIRouter(prefix="/profiles/{profile_id}/schedule", tags=["schedule"])

_DAY_NAMES = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]


def _ensure_schedule_rows(profile_id: int, db):
    """Guarantee all 7 schedule rows exist for this profile."""
    for dow in range(7):
        db.execute(
            "INSERT OR IGNORE INTO schedule (profile_id, day_of_week) VALUES (?,?)",
            (profile_id, dow),
        )
    db.commit()


def _schedule_row(row: dict, db) -> dict:
    """Enrich a raw schedule row with nested workout_day and diet_day objects."""
    out = dict(row)
    out["day_name"] = _DAY_NAMES[row["day_of_week"]]

    wid = row["workout_day_id"]
    if wid:
        wd = db.execute("SELECT * FROM workout_days WHERE id=?", (wid,)).fetchone()
        if wd:
            blocks = db.execute(
                """SELECT wb.*, wdb.id AS entry_id, wdb.sort_order
                   FROM workout_day_blocks wdb
                   JOIN workout_blocks wb ON wb.id = wdb.workout_block_id
                   WHERE wdb.workout_day_id = ?
                   ORDER BY wdb.sort_order""",
                (wid,),
            ).fetchall()
            out["workout_day"] = {**dict(wd), "blocks": [dict(b) for b in blocks]}
        else:
            out["workout_day"] = None
    else:
        out["workout_day"] = None

    did = row["diet_day_id"]
    if did:
        dd = db.execute("SELECT * FROM diet_days WHERE id=?", (did,)).fetchone()
        if dd:
            meals = db.execute(
                """SELECT mb.*, ddm.id AS entry_id, ddm.sort_order
                   FROM diet_day_meals ddm
                   JOIN meal_blocks mb ON mb.id = ddm.meal_block_id
                   WHERE ddm.diet_day_id = ?
                   ORDER BY ddm.sort_order""",
                (did,),
            ).fetchall()
            out["diet_day"] = {**dict(dd), "meals": [dict(m) for m in meals]}
        else:
            out["diet_day"] = None
    else:
        out["diet_day"] = None

    return out


@router.get("")
def get_schedule(profile_id: int, db: sqlite3.Connection = Depends(get_db)):
    _ensure_schedule_rows(profile_id, db)
    rows = db.execute(
        "SELECT * FROM schedule WHERE profile_id=? ORDER BY day_of_week",
        (profile_id,),
    ).fetchall()
    return [_schedule_row(dict(r), db) for r in rows]


@router.put("/{day_of_week}")
def update_schedule_day(
    profile_id: int,
    day_of_week: int,
    body: ScheduleDayUpdate,
    db: sqlite3.Connection = Depends(get_db),
):
    if day_of_week < 0 or day_of_week > 6:
        raise HTTPException(400, "day_of_week must be 0 (Monday) – 6 (Sunday)")

    _ensure_schedule_rows(profile_id, db)

    # Validate that referenced days belong to this profile
    if body.workout_day_id is not None:
        if not db.execute(
            "SELECT id FROM workout_days WHERE id=? AND profile_id=?",
            (body.workout_day_id, profile_id),
        ).fetchone():
            raise HTTPException(404, "Workout day not found")

    if body.diet_day_id is not None:
        if not db.execute(
            "SELECT id FROM diet_days WHERE id=? AND profile_id=?",
            (body.diet_day_id, profile_id),
        ).fetchone():
            raise HTTPException(404, "Diet day not found")

    row = db.execute(
        "SELECT * FROM schedule WHERE profile_id=? AND day_of_week=?",
        (profile_id, day_of_week),
    ).fetchone()
    cur = dict(row)

    # None in body means "clear this slot"; omitted (not provided) would also be None
    # Both cases set the column to NULL — caller must pass the existing ID to preserve it
    new_wid = body.workout_day_id
    new_did = body.diet_day_id

    db.execute(
        "UPDATE schedule SET workout_day_id=?, diet_day_id=? WHERE profile_id=? AND day_of_week=?",
        (new_wid, new_did, profile_id, day_of_week),
    )
    db.commit()

    updated = db.execute(
        "SELECT * FROM schedule WHERE profile_id=? AND day_of_week=?",
        (profile_id, day_of_week),
    ).fetchone()
    return _schedule_row(dict(updated), db)
