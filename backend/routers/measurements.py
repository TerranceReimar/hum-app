import sqlite3
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException
from database import get_db
from models import MeasurementTypeCreate, MetricCreate

router = APIRouter(tags=["measurements"])


# ── Measurement types ─────────────────────────────────────────────────────────

@router.get("/measurement-types")
def list_measurement_types(db: sqlite3.Connection = Depends(get_db)):
    rows = db.execute(
        "SELECT * FROM measurement_types ORDER BY name"
    ).fetchall()
    return [dict(r) for r in rows]


@router.post("/measurement-types", status_code=201)
def create_measurement_type(
    body: MeasurementTypeCreate,
    db: sqlite3.Connection = Depends(get_db),
):
    try:
        cur = db.execute(
            "INSERT INTO measurement_types (name) VALUES (?)", (body.name,)
        )
        db.commit()
        return {"id": cur.lastrowid, "name": body.name}
    except sqlite3.IntegrityError:
        raise HTTPException(400, "Already exists")


@router.delete("/measurement-types/{type_id}", status_code=204)
def delete_measurement_type(
    type_id: int,
    db: sqlite3.Connection = Depends(get_db),
):
    db.execute("DELETE FROM measurement_types WHERE id=?", (type_id,))
    db.commit()


# ── Weekly metrics per profile ────────────────────────────────────────────────

@router.get("/profiles/{profile_id}/metrics")
def list_metrics(
    profile_id: int,
    metric_type: str = None,
    limit: int = 100,
    db: sqlite3.Connection = Depends(get_db),
):
    if metric_type:
        rows = db.execute(
            """SELECT * FROM user_metrics
               WHERE profile_id=? AND metric_type=?
               ORDER BY timestamp DESC LIMIT ?""",
            (profile_id, metric_type, limit),
        ).fetchall()
    else:
        rows = db.execute(
            """SELECT * FROM user_metrics
               WHERE profile_id=?
               ORDER BY timestamp DESC LIMIT ?""",
            (profile_id, limit),
        ).fetchall()
    return [dict(r) for r in rows]


@router.post("/profiles/{profile_id}/metrics", status_code=201)
def log_metric(
    profile_id: int,
    body: MetricCreate,
    db: sqlite3.Connection = Depends(get_db),
):
    ts = body.timestamp or datetime.utcnow().isoformat()
    cur = db.execute(
        """INSERT INTO user_metrics (profile_id, timestamp, metric_type, value, notes)
           VALUES (?,?,?,?,?)""",
        (profile_id, ts, body.metric_type, body.value, body.notes),
    )
    db.commit()
    return {"id": cur.lastrowid, "timestamp": ts}


@router.delete("/profiles/{profile_id}/metrics/{metric_id}", status_code=204)
def delete_metric(
    profile_id: int,
    metric_id: int,
    db: sqlite3.Connection = Depends(get_db),
):
    db.execute(
        "DELETE FROM user_metrics WHERE id=? AND profile_id=?",
        (metric_id, profile_id),
    )
    db.commit()
