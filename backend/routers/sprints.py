import sqlite3
from datetime import date, timedelta
from fastapi import APIRouter, Depends, HTTPException
from database import get_db, get_current_sprint

router = APIRouter(prefix="/sprints", tags=["sprints"])


# ── Helpers ───────────────────────────────────────────────────────────────────

def _scheduled_days_in_range(profile_id: int, start: date, end: date, db) -> int:
    """
    Count how many calendar days in [start, end] have at least one workout_day
    or diet_day assigned in this profile's schedule.
    """
    rows = db.execute(
        """SELECT day_of_week FROM schedule
           WHERE profile_id=?
             AND (workout_day_id IS NOT NULL OR diet_day_id IS NOT NULL)""",
        (profile_id,),
    ).fetchall()
    scheduled_dows = {r["day_of_week"] for r in rows}
    if not scheduled_dows:
        return 0

    count = 0
    cursor = start
    while cursor <= end:
        if cursor.weekday() in scheduled_dows:
            count += 1
        cursor += timedelta(days=1)
    return count


def _sprint_score(profile_id: int, sprint: dict, db) -> float:
    """
    sprint_score = (sum of daily_log.score for days in sprint) / total_scheduled_days * 100
    Unlogged scheduled days contribute 0 to the sum.
    Returns 0.0 if no scheduled days.
    """
    start = date.fromisoformat(sprint["start_date"])
    today = date.today()
    end_cap = min(date.fromisoformat(sprint["end_date"]), today)

    if start > end_cap:
        return 0.0

    total_scheduled = _scheduled_days_in_range(profile_id, start, end_cap, db)
    if total_scheduled == 0:
        return 0.0

    rows = db.execute(
        """SELECT score FROM daily_logs
           WHERE profile_id=? AND date >= ? AND date <= ?""",
        (profile_id, sprint["start_date"], end_cap.isoformat()),
    ).fetchall()
    sum_scores = sum(r["score"] for r in rows)
    return round((sum_scores / total_scheduled) * 100, 2)


# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.get("/current")
def current_sprint(db: sqlite3.Connection = Depends(get_db)):
    sprint = get_current_sprint(db)
    if not sprint:
        raise HTTPException(503, "No active sprint")
    today = date.today()
    start = date.fromisoformat(sprint["start_date"])
    end = date.fromisoformat(sprint["end_date"])
    days_elapsed = (today - start).days + 1
    days_total = (end - start).days + 1
    return {
        **sprint,
        "days_elapsed": days_elapsed,
        "days_total": days_total,
        "days_remaining": max(0, days_total - days_elapsed),
    }


@router.get("/current/leaderboard")
def leaderboard(db: sqlite3.Connection = Depends(get_db)):
    sprint = get_current_sprint(db)
    if not sprint:
        raise HTTPException(503, "No active sprint")

    profiles = db.execute("SELECT id, name FROM profiles ORDER BY name").fetchall()
    board = []
    for p in profiles:
        score = _sprint_score(p["id"], sprint, db)
        board.append({"profile_id": p["id"], "name": p["name"], "score": score})

    board.sort(key=lambda x: x["score"], reverse=True)
    for i, entry in enumerate(board):
        entry["rank"] = i + 1

    return {"sprint": sprint, "leaderboard": board}


@router.get("/current/profile/{profile_id}")
def profile_sprint_detail(
    profile_id: int,
    db: sqlite3.Connection = Depends(get_db),
):
    """Return this profile's score + daily breakdown for the current sprint."""
    sprint = get_current_sprint(db)
    if not sprint:
        raise HTTPException(503, "No active sprint")

    score = _sprint_score(profile_id, sprint, db)

    logs = db.execute(
        """SELECT date, score, note FROM daily_logs
           WHERE profile_id=? AND date >= ? AND date <= ?
           ORDER BY date""",
        (profile_id, sprint["start_date"], sprint["end_date"]),
    ).fetchall()

    return {
        "sprint": sprint,
        "score": score,
        "logs": [dict(r) for r in logs],
    }
