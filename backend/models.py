from pydantic import BaseModel
from typing import Optional, List


# ── Profiles ──────────────────────────────────────────────────────────────────

class ProfileCreate(BaseModel):
    name: str
    password: str
    notification_time: str = "20:00"
    goal_weight: Optional[float] = None
    goal_waist:  Optional[float] = None
    goal_arm:    Optional[float] = None
    goal_chest:  Optional[float] = None
    goal_thigh:  Optional[float] = None
    goal_neck:   Optional[float] = None
    goal_hip:    Optional[float] = None


class ProfileUpdate(BaseModel):
    name:              Optional[str]   = None
    password:          Optional[str]   = None
    notification_time: Optional[str]   = None
    goal_weight:       Optional[float] = None
    goal_waist:        Optional[float] = None
    goal_arm:          Optional[float] = None
    goal_chest:        Optional[float] = None
    goal_thigh:        Optional[float] = None
    goal_neck:         Optional[float] = None
    goal_hip:          Optional[float] = None


class PasswordVerify(BaseModel):
    password: str


# ── Exercise library ──────────────────────────────────────────────────────────

class ExerciseCreate(BaseModel):
    name: str
    category: str = "Other"


class ExerciseUpdate(BaseModel):
    name:     Optional[str] = None
    category: Optional[str] = None


# ── Measurement types + weekly metrics ────────────────────────────────────────

class MeasurementTypeCreate(BaseModel):
    name: str


class MetricCreate(BaseModel):
    metric_type: str
    value:       float
    notes:       str = ""
    timestamp:   Optional[str] = None


# ── Workout blocks ────────────────────────────────────────────────────────────

class WorkoutBlockCreate(BaseModel):
    exercise_name: str
    sets:  int
    reps:  int
    notes: str = ""


class WorkoutBlockUpdate(BaseModel):
    exercise_name: Optional[str] = None
    sets:          Optional[int] = None
    reps:          Optional[int] = None
    notes:         Optional[str] = None


# ── Workout days ──────────────────────────────────────────────────────────────

class WorkoutDayCreate(BaseModel):
    name: str


class WorkoutDayBlockAdd(BaseModel):
    workout_block_id: int
    sort_order:       int = 0


class ReorderItems(BaseModel):
    # List of join-table entry IDs (workout_day_blocks.id or diet_day_meals.id)
    # in the desired display order.
    ordered_ids: List[int]


# ── Meal blocks ───────────────────────────────────────────────────────────────

class MealBlockCreate(BaseModel):
    name:        str
    description: str   = ""
    calories:    int   = 0
    protein_g:   float = 0
    carbs_g:     float = 0
    fat_g:       float = 0


class MealBlockUpdate(BaseModel):
    name:        Optional[str]   = None
    description: Optional[str]   = None
    calories:    Optional[int]   = None
    protein_g:   Optional[float] = None
    carbs_g:     Optional[float] = None
    fat_g:       Optional[float] = None


# ── Diet days ─────────────────────────────────────────────────────────────────

class DietDayCreate(BaseModel):
    name: str


class DietDayMealAdd(BaseModel):
    meal_block_id: int
    sort_order:    int = 0


# ── Weekly schedule ───────────────────────────────────────────────────────────

class ScheduleDayUpdate(BaseModel):
    # Pass None to clear (mark as rest). Omitting a key leaves it unchanged.
    workout_day_id: Optional[int] = None
    diet_day_id:    Optional[int] = None


# ── Daily confirmation ────────────────────────────────────────────────────────

class DailyLogBlockInput(BaseModel):
    block_type: str   # 'workout' | 'diet'
    block_id:   int
    completed:  bool
    floor_only: bool = False


class DailyLogCreate(BaseModel):
    date:     str                       # YYYY-MM-DD
    all_met:  bool                      = False
    blocks:   List[DailyLogBlockInput]  = []
    note:     str                       = ""
