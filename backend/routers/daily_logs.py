import sqlite3
from datetime import date
from fastapi import APIRouter, Depends, HTTPException
from database import get_db
from models import DailyLogCreate

router = APIRouter(prefix="/profiles/{profile_id}", tags=["daily-logs"])

_DAY_NAMES = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]


# ── Helpers ───────────────────────────────────────────────────────────────────

def _compute_score(blocks: list) -> float:
    """Average block scores. Full=1.0, floor_only=0.5, missed=0.0."""
    if not blocks:
        return 0.0
    total = sum(
        (1.0 if b.completed and not b.floor_only else 0.5 if b.completed else 0.0)
        for b in blocks
    )
    return total / len(blocks)


def _log_detail(log_row: dict, db) -> dict:
    """Add block detail to a raw daily_logs row."""
    entries = db.execute(
        "SELECT * FROM daily_log_blocks WHERE daily_log_id=?", (log_row["id"],)
    ).fetchall()
    return {**log_row, "blocks": [dict(e) for e in entries]}


def _today_plan(profile_id: int, today: date, db) -> dict:
    """Return today's scheduled workout_day and diet_day with their blocks/meals."""
    dow = today.weekday()  # 0=Monday … 6=Sunday

    row = db.execute(
        "SELECT * FROM schedule WHERE profile_id=? AND day_of_week=?",
        (profile_id, dow),
    ).fetchone()

    plan: dict = {
        "date": today.isoformat(),
        "day_of_week": dow,
        "day_name": _DAY_NAMES[dow],
        "workout_day": None,
        "diet_day": None,
    }

    if not row:
        return plan

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
            plan["workout_day"] = {**dict(wd), "blocks": [dict(b) for b in blocks]}

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
            plan["diet_day"] = {**dict(dd), "meals": [dict(m) for m in meals]}

    return plan


# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.get("/today")
def get_today(profile_id: int, db: sqlite3.Connection = Depends(get_db)):
    """Return today's scheduled plan for the profile."""
    today = date.today()
    plan = _today_plan(profile_id, today, db)

    # Attach existing log if already submitted
    log_row = db.execute(
        "SELECT * FROM daily_logs WHERE profile_id=? AND date=?",
        (profile_id, today.isoformat()),
    ).fetchone()
    plan["log"] = _log_detail(dict(log_row), db) if log_row else None

    return plan


@router.post("/daily-logs", status_code=201)
def submit_daily_log(
    profile_id: int,
    body: DailyLogCreate,
    db: sqlite3.Connection = Depends(get_db),
):
    """Submit (or overwrite) today's daily confirmation."""
    # Check for existing log and remove it (allow re-submission)
    existing = db.execute(
        "SELECT id FROM daily_logs WHERE profile_id=? AND date=?",
        (profile_id, body.date),
    ).fetchone()
    if existing:
        db.execute("DELETE FROM daily_logs WHERE id=?", (existing["id"],))

    score = _compute_score(body.blocks) if body.blocks else (1.0 if body.all_met else 0.0)

    cur = db.execute(
        "INSERT INTO daily_logs (profile_id, date, score, note) VALUES (?,?,?,?)",
        (profile_id, body.date, score, body.note),
    )
    log_id = cur.lastrowid

    for b in body.blocks:
        db.execute(
            """INSERT INTO daily_log_blocks
               (daily_log_id, block_type, block_id, completed, floor_only)
               VALUES (?,?,?,?,?)""",
            (log_id, b.block_type, b.block_id, int(b.completed), int(b.floor_only)),
        )

    db.commit()
    row = db.execute("SELECT * FROM daily_logs WHERE id=?", (log_id,)).fetchone()
    return _log_detail(dict(row), db)


@router.get("/daily-logs")
def list_daily_logs(
    profile_id: int,
    limit: int = 90,
    db: sqlite3.Connection = Depends(get_db),
):
    """Recent log history, newest first."""
    rows = db.execute(
        """SELECT * FROM daily_logs
           WHERE profile_id=?
           ORDER BY date DESC LIMIT ?""",
        (profile_id, limit),
    ).fetchall()
    return [_log_detail(dict(r), db) for r in rows]


@router.get("/daily-logs/{log_date}")
def get_daily_log(
    profile_id: int,
    log_date: str,
    db: sqlite3.Connection = Depends(get_db),
):
    """Get a specific day's log by YYYY-MM-DD date string."""
    row = db.execute(
        "SELECT * FROM daily_logs WHERE profile_id=? AND date=?",
        (profile_id, log_date),
    ).fetchone()
    if not row:
        raise HTTPException(404, "No log found for this date")
    return _log_detail(dict(row), db)
