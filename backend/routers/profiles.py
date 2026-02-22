import sqlite3
from fastapi import APIRouter, Depends, HTTPException
from database import get_db
from models import ProfileCreate, ProfileUpdate, PasswordVerify

router = APIRouter(prefix="/profiles", tags=["profiles"])

_FIELDS = [
    "name", "password", "notification_time",
    "goal_weight", "goal_waist", "goal_arm", "goal_chest",
    "goal_thigh", "goal_neck", "goal_hip",
]


def _profile_out(row) -> dict:
    d = dict(row)
    password = d.pop("password", "")
    d["has_password"] = bool(password)
    return d


@router.get("")
def list_profiles(db: sqlite3.Connection = Depends(get_db)):
    rows = db.execute("SELECT * FROM profiles ORDER BY name").fetchall()
    return [_profile_out(r) for r in rows]


@router.post("", status_code=201)
def create_profile(body: ProfileCreate, db: sqlite3.Connection = Depends(get_db)):
    try:
        cur = db.execute(
            """INSERT INTO profiles
               (name, password, notification_time,
                goal_weight, goal_waist, goal_arm, goal_chest,
                goal_thigh, goal_neck, goal_hip)
               VALUES (?,?,?,?,?,?,?,?,?,?)""",
            (body.name, body.password, body.notification_time,
             body.goal_weight, body.goal_waist, body.goal_arm, body.goal_chest,
             body.goal_thigh, body.goal_neck, body.goal_hip),
        )
        db.commit()
        row = db.execute("SELECT * FROM profiles WHERE id=?", (cur.lastrowid,)).fetchone()
        return _profile_out(row)
    except sqlite3.IntegrityError:
        raise HTTPException(400, "Profile name already exists")


@router.get("/{profile_id}")
def get_profile(profile_id: int, db: sqlite3.Connection = Depends(get_db)):
    row = db.execute("SELECT * FROM profiles WHERE id=?", (profile_id,)).fetchone()
    if not row:
        raise HTTPException(404, "Profile not found")
    return _profile_out(row)


@router.post("/{profile_id}/verify-password")
def verify_password(
    profile_id: int,
    body: PasswordVerify,
    db: sqlite3.Connection = Depends(get_db),
):
    row = db.execute(
        "SELECT password FROM profiles WHERE id=?", (profile_id,)
    ).fetchone()
    if not row:
        raise HTTPException(404, "Profile not found")
    return {"valid": row["password"] == body.password}


@router.put("/{profile_id}")
def update_profile(
    profile_id: int,
    body: ProfileUpdate,
    db: sqlite3.Connection = Depends(get_db),
):
    row = db.execute("SELECT * FROM profiles WHERE id=?", (profile_id,)).fetchone()
    if not row:
        raise HTTPException(404, "Profile not found")
    current = dict(row)
    updates = {
        f: getattr(body, f) if getattr(body, f) is not None else current.get(f)
        for f in _FIELDS
    }
    db.execute(
        f"UPDATE profiles SET {', '.join(f + '=?' for f in _FIELDS)} WHERE id=?",
        [*updates.values(), profile_id],
    )
    db.commit()
    updated = db.execute("SELECT * FROM profiles WHERE id=?", (profile_id,)).fetchone()
    return _profile_out(updated)


@router.delete("/{profile_id}", status_code=204)
def delete_profile(profile_id: int, db: sqlite3.Connection = Depends(get_db)):
    db.execute("DELETE FROM profiles WHERE id=?", (profile_id,))
    db.commit()
