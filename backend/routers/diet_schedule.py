import sqlite3
from fastapi import APIRouter, Depends, HTTPException
from database import get_db
from models import MealBlockCreate, MealBlockUpdate, DietDayCreate, DietDayMealAdd, ReorderItems

router = APIRouter(prefix="/profiles/{profile_id}", tags=["diet-schedule"])


# ── Helpers ───────────────────────────────────────────────────────────────────

def _day_with_meals(day_id: int, db) -> dict | None:
    day = db.execute("SELECT * FROM diet_days WHERE id=?", (day_id,)).fetchone()
    if not day:
        return None
    meals = db.execute(
        """SELECT mb.*, ddm.id AS entry_id, ddm.sort_order
           FROM diet_day_meals ddm
           JOIN meal_blocks mb ON mb.id = ddm.meal_block_id
           WHERE ddm.diet_day_id = ?
           ORDER BY ddm.sort_order""",
        (day_id,),
    ).fetchall()
    return {**dict(day), "meals": [dict(m) for m in meals]}


# ── Meal blocks ───────────────────────────────────────────────────────────────

@router.get("/meal-blocks")
def list_meal_blocks(profile_id: int, db: sqlite3.Connection = Depends(get_db)):
    rows = db.execute(
        "SELECT * FROM meal_blocks WHERE profile_id=? ORDER BY name",
        (profile_id,),
    ).fetchall()
    return [dict(r) for r in rows]


@router.post("/meal-blocks", status_code=201)
def create_meal_block(
    profile_id: int,
    body: MealBlockCreate,
    db: sqlite3.Connection = Depends(get_db),
):
    cur = db.execute(
        """INSERT INTO meal_blocks
           (profile_id, name, description, calories, protein_g, carbs_g, fat_g)
           VALUES (?,?,?,?,?,?,?)""",
        (profile_id, body.name, body.description, body.calories,
         body.protein_g, body.carbs_g, body.fat_g),
    )
    db.commit()
    return dict(db.execute("SELECT * FROM meal_blocks WHERE id=?", (cur.lastrowid,)).fetchone())


@router.put("/meal-blocks/{block_id}")
def update_meal_block(
    profile_id: int,
    block_id: int,
    body: MealBlockUpdate,
    db: sqlite3.Connection = Depends(get_db),
):
    row = db.execute(
        "SELECT * FROM meal_blocks WHERE id=? AND profile_id=?", (block_id, profile_id)
    ).fetchone()
    if not row:
        raise HTTPException(404, "Meal block not found")
    cur = dict(row)
    db.execute(
        """UPDATE meal_blocks
           SET name=?, description=?, calories=?, protein_g=?, carbs_g=?, fat_g=?
           WHERE id=?""",
        (
            body.name        if body.name        is not None else cur["name"],
            body.description if body.description is not None else cur["description"],
            body.calories    if body.calories    is not None else cur["calories"],
            body.protein_g   if body.protein_g   is not None else cur["protein_g"],
            body.carbs_g     if body.carbs_g     is not None else cur["carbs_g"],
            body.fat_g       if body.fat_g       is not None else cur["fat_g"],
            block_id,
        ),
    )
    db.commit()
    return dict(db.execute("SELECT * FROM meal_blocks WHERE id=?", (block_id,)).fetchone())


@router.delete("/meal-blocks/{block_id}", status_code=204)
def delete_meal_block(
    profile_id: int,
    block_id: int,
    db: sqlite3.Connection = Depends(get_db),
):
    db.execute(
        "DELETE FROM meal_blocks WHERE id=? AND profile_id=?", (block_id, profile_id)
    )
    db.commit()


# ── Diet days ─────────────────────────────────────────────────────────────────

@router.get("/diet-days")
def list_diet_days(profile_id: int, db: sqlite3.Connection = Depends(get_db)):
    days = db.execute(
        "SELECT * FROM diet_days WHERE profile_id=? ORDER BY name", (profile_id,)
    ).fetchall()
    return [_day_with_meals(d["id"], db) for d in days]


@router.post("/diet-days", status_code=201)
def create_diet_day(
    profile_id: int,
    body: DietDayCreate,
    db: sqlite3.Connection = Depends(get_db),
):
    cur = db.execute(
        "INSERT INTO diet_days (profile_id, name) VALUES (?,?)", (profile_id, body.name)
    )
    db.commit()
    return _day_with_meals(cur.lastrowid, db)


@router.put("/diet-days/{day_id}")
def update_diet_day(
    profile_id: int,
    day_id: int,
    body: DietDayCreate,
    db: sqlite3.Connection = Depends(get_db),
):
    row = db.execute(
        "SELECT id FROM diet_days WHERE id=? AND profile_id=?", (day_id, profile_id)
    ).fetchone()
    if not row:
        raise HTTPException(404, "Diet day not found")
    db.execute("UPDATE diet_days SET name=? WHERE id=?", (body.name, day_id))
    db.commit()
    return _day_with_meals(day_id, db)


@router.delete("/diet-days/{day_id}", status_code=204)
def delete_diet_day(
    profile_id: int,
    day_id: int,
    db: sqlite3.Connection = Depends(get_db),
):
    db.execute(
        "DELETE FROM diet_days WHERE id=? AND profile_id=?", (day_id, profile_id)
    )
    db.commit()


# ── Meals within a day ────────────────────────────────────────────────────────

@router.post("/diet-days/{day_id}/meals", status_code=201)
def add_meal_to_day(
    profile_id: int,
    day_id: int,
    body: DietDayMealAdd,
    db: sqlite3.Connection = Depends(get_db),
):
    if not db.execute(
        "SELECT id FROM diet_days WHERE id=? AND profile_id=?", (day_id, profile_id)
    ).fetchone():
        raise HTTPException(404, "Diet day not found")
    if not db.execute(
        "SELECT id FROM meal_blocks WHERE id=? AND profile_id=?",
        (body.meal_block_id, profile_id),
    ).fetchone():
        raise HTTPException(404, "Meal block not found")
    db.execute(
        "INSERT INTO diet_day_meals (diet_day_id, meal_block_id, sort_order) VALUES (?,?,?)",
        (day_id, body.meal_block_id, body.sort_order),
    )
    db.commit()
    return _day_with_meals(day_id, db)


@router.delete("/diet-days/{day_id}/meals/{entry_id}", status_code=204)
def remove_meal_from_day(
    profile_id: int,
    day_id: int,
    entry_id: int,
    db: sqlite3.Connection = Depends(get_db),
):
    db.execute(
        "DELETE FROM diet_day_meals WHERE id=? AND diet_day_id=?",
        (entry_id, day_id),
    )
    db.commit()


@router.put("/diet-days/{day_id}/meals/reorder")
def reorder_day_meals(
    profile_id: int,
    day_id: int,
    body: ReorderItems,
    db: sqlite3.Connection = Depends(get_db),
):
    for i, entry_id in enumerate(body.ordered_ids):
        db.execute(
            "UPDATE diet_day_meals SET sort_order=? WHERE id=? AND diet_day_id=?",
            (i, entry_id, day_id),
        )
    db.commit()
    return _day_with_meals(day_id, db)
