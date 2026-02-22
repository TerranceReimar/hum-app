# Hum — Product Plan v1.0

> All decisions resolved. Ready to build.

---

## Core Philosophy

The app has one job: **make it dead simple to stay consistent**.

Users don't log workouts in detail. They don't count calories in real time. They build a schedule once, live their life, and at the end of each day answer one question: *Did you hit your goals today? If not, what did you miss?*

Everything else — ranking, sprints, accountability — flows from that daily confirmation.

---

## 1. Workouts

### How it works
The user defines a weekly schedule once. Each day maps to a **Workout Day** (a named collection of exercise blocks) or is a rest day. The home screen surfaces today's plan. At the end of the day the user confirms.

### Workout Block
The atomic unit. Represents one exercise:

| Field | Notes |
|---|---|
| Exercise name | From the exercise library |
| Sets | e.g. 4 |
| Reps | e.g. 8 |
| Effort floor *(optional)* | Minimum acceptable version, e.g. 3 × 6 |
| Notes *(optional)* | e.g. "Weighted +10kg" |

The **effort floor** is a stretch feature — it lets a user define the bare minimum that still "counts" for a block. If only the floor is hit, that block is marked partial rather than skipped.

Blocks are composed into a **Workout Day**:
```
Upper Body
  ├─ 3 × 12 Pullups          [floor: 2 × 8]
  ├─ 4 × 12 Hammer Curls
  └─ 4 × 8 Barbell Rows
```

### Weekly Schedule
```
Monday    → Upper Body
Tuesday   → Legs
Wednesday → Rest
Thursday  → Upper Body
Friday    → Legs
Saturday  → Rest
Sunday    → Rest
```

### Notifications
If unconfirmed by **8:00 PM**, a push notification fires:
> *"Upper Body day — did you hit it? Log before midnight."*
If both workout and diet are unconfirmed, one combined notification fires, not two.

Notification time is **configurable per profile** (default 8:00 PM). ✅

---

## 2. Diet

Mirrors the workout system. Same philosophy, same confirmation flow.

### Meal Block
| Field | Example |
|---|---|
| Name | Lunch |
| Description / Recipe | Chicken, rice, broccoli |
| Calories | 650 kcal |
| Protein | 52g |
| Carbs | 60g |
| Fat | 14g |

### Diet Day
```
Monday Diet
  ├─ Breakfast — Oats + protein shake    (420 kcal · 38p · 52c · 8f)
  ├─ Lunch     — Chicken & rice          (650 kcal · 52p · 60c · 14f)
  └─ Dinner    — Salmon & veg            (480 kcal · 44p · 20c · 18f)
  ─────────────────────────────────────────────────────────────────
  Total: 1550 kcal · 134p · 132c · 40f
```

Workout and diet schedules are **configured independently** — separate builders, separate week grids. ✅

---

## 3. The Partial State

> This is one of the most important features in the app. Designed well, it turns "I slipped today" into useful signal and keeps people from quitting entirely.

### The Confirmation Flow ✅

The daily confirmation is **one combined prompt**, not separate per category:

```
┌─────────────────────────────────────────┐
│  How did today go?                      │
│                                         │
│  [✓ All goals met]                      │
│                                         │
│  [Something was off ▼]                  │
│    ☑ Workout                            │
│      ☑ 3 × 12 Pullups                   │
│      ☑ 4 × 12 Hammer Curls              │
│      ☐ 4 × 8 Barbell Rows  ← skipped   │
│    ☑ Diet                               │
│      ☑ Breakfast                        │
│      ☐ Lunch               ← skipped   │
│      ☑ Dinner                           │
│                                         │
│  Note (optional): ________________      │
│                                         │
│  [Submit]                               │
└─────────────────────────────────────────┘
```

"What did you miss?" is answered implicitly by unchecking blocks. No extra steps.

### Day Score

Each day receives a **score from 0.0 to 1.0** based on block completion:

```
score = (blocks checked) / (total scheduled blocks)
```

Workout blocks and diet blocks are weighted equally by default.

| Example | Score |
|---|---|
| All 7 blocks checked | 1.0 → Full (dark green) |
| 5 of 7 blocks checked | 0.71 → Partial (light green) |
| 0 blocks / not confirmed | 0.0 → Miss (red) |
| Rest day | — (grey, not counted) |

Workout and diet blocks are weighted **equally** (50/50). ✅

### Grid Colours ✅

| State | Colour | Meaning |
|---|---|---|
| Full | Bright neon green `#CCFF00` | Score = 1.0 |
| Partial | Muted olive green `#667700` | Score 0.01–0.99 |
| Miss | Red `#FF4444` | Score = 0.0 or no confirmation |
| Rest / Future | Dark grey `#1C1C1C` | Not a scheduled day |

### Ideas to Build On

These are features that grow naturally from block-level partial tracking, roughly ordered easiest → most ambitious:

---

**A. Partial recovery window**
After logging partial, a subtle nudge appears if it's before 10 PM: *"You still have time — finish those last 2 blocks and upgrade today to green."* Tapping it reopens the confirmation screen. This gamifies finishing the day strong without being aggressive.

---

**B. Skip analytics**
Because the app knows which specific blocks were skipped and when, it can surface patterns:

> *"You've skipped Barbell Rows 5 times this sprint. Consider moving it earlier in your Upper Body session."*

Over a sprint, a heatmap on the Manage screen shows each block coloured by skip frequency. This turns partial data into actionable schedule feedback — the app helps you improve your own programming.

---

**C. Momentum indicator**
Rather than treating a string of partials as a slow death spiral, the app recognises the pattern and reframes it:

- 3+ consecutive partial days → grid cells pulse slightly, a small "building momentum" label appears on the today screen
- Upgrading a partial streak to a full green day triggers a satisfying animation

The idea: partial is not failure, it's below-target effort. Momentum says *you're still in it*.

---

**D. Sprint scoring by consistency percentile**
Because everyone has different schedules (5 workout days vs 7), the sprint leaderboard ranks by **average daily score** across all scheduled days, not raw green count:

```
sprint_score = sum(daily_scores) / total_scheduled_days × 100
```

This means:
- Someone who hits 90% of every single day outranks someone who goes full green on workout days but ignores diet
- Partial days contribute real points — they are not the same as a miss
- People with heavier schedules aren't punished for having more blocks to complete

---

**E. Comeback mechanic (stretch)**
If a user logs a miss (score 0.0) followed by two consecutive full greens, the missed day's cell gains a small gold border on the grid — a visual "comeback" marker. This isn't retroactively worth more points, but it tells a story: *I fell, and I came back.*

---

**F. Effort floor per block (stretch)**
Each exercise block can optionally define a minimum version of itself. If the floor was met but the full prescription wasn't, that block counts as 0.5 rather than 0 or 1. Example:
- Prescribed: 4 × 8 Barbell Rows
- Floor: 3 × 5 Rows
- Did 3 × 5 → block score: 0.5

This rewards doing *something* rather than nothing, and allows the schedule builder to define "non-negotiable floor" exercises vs optional ones.

---

## 4. Measurements

### Cadence
Once per week. The app prompts on a user-chosen day (default: Sunday).

The prompt is a **push notification** (same configurable time as the daily goal reminder). ✅

### Inputs
Same fields as today: weight, waist, arm, chest, thigh, neck, hip. Surfaced as a structured weekly check-in form, not ad hoc logging.

### What it drives
- A progress chart on the profile screen showing each measurement over time
- Starting vs current deltas shown clearly ("−2.5 cm waist since start")
- No scoring impact — measurements are purely informational and self-directed

Measurements are **purely graphical** — no generated text insights. ✅

---

## 5. Accountability / Social

### The Grid ✅

Each profile has a 90-day activity grid (GitHub-style). Each cell = one day, coloured by score. All profiles can see all grids. No opt-out.

```
◼◼🟩🟩🟩🟨🟩🟩🟥🟩🟩🟩🟨🟩...
```

Tapping a cell shows that day's detail: which blocks were hit, which were missed, any note left.

### Sprints ✅

Sprints are **global** — same 90-day window for all profiles on the server. Everyone runs together.

- The server defines sprint start/end dates
- All daily logs feed into the current sprint
- At sprint end, a leaderboard is computed and frozen
- A new sprint begins immediately

**Sprint leaderboard** ranks profiles by `sprint_score` (see scoring above). The leaderboard is public — no opt-out. We're all in this together.

Only the **current sprint** leaderboard is shown. Past sprints are not archived. ✅
Sprints **auto-advance** — Sprint N+1 starts the Monday immediately after Sprint N ends, forever. No admin intervention needed. ✅

### The Herd Dynamic

The global sprint creates a shared cadence. Everyone knows when the sprint resets. The leaderboard incentivises not falling too far behind — it's not about being first, it's about not being last. Ranking by percentile means you're always competing against the whole group, regardless of schedule intensity.

---

## Screen Structure

```
Dashboard
├─ Today tab    → Today's workout + diet plan, confirm button, measurement nudge if due
├─ History tab  → 90-day grid for self + browse other profiles' grids
└─ Manage tab   → Schedule builder (workout days, diet days, week mapping, block library)

More
├─ Profile tab  → Measurements chart, body stats, sprint history
└─ Settings tab → Server URL, notification time, sprint info
```

---

## Data Model

### Core tables

**`workout_blocks`** — reusable exercise blocks owned by a profile
```
id, profile_id, exercise_name, sets, reps, floor_sets, floor_reps, notes
```

**`workout_days`** — named collections of blocks
```
id, profile_id, name
```

**`workout_day_blocks`** — ordered join
```
id, workout_day_id, workout_block_id, sort_order
```

**`meal_blocks`** — reusable meal blocks owned by a profile
```
id, profile_id, name, description, calories, protein_g, carbs_g, fat_g
```

**`diet_days`**
```
id, profile_id, name
```

**`diet_day_meals`** — ordered join
```
id, diet_day_id, meal_block_id, sort_order
```

**`schedule`** — one row per day of week per profile
```
id, profile_id, day_of_week (0=Mon … 6=Sun), workout_day_id (null = rest), diet_day_id (null = rest)
```

**`daily_logs`** — the core record
```
id, profile_id, date, score (REAL 0.0–1.0), note (TEXT)
```

**`daily_log_blocks`** — which blocks were hit/missed on a given day
```
id, daily_log_id, block_type (workout|diet), block_id, completed (BOOL), floor_only (BOOL)
```

**`sprints`**
```
id, start_date, end_date
```

**`sprint_rankings`** — computed and frozen at sprint end
```
id, sprint_id, profile_id, sprint_score (REAL), rank (INT)
```

---

## What Changes vs What Exists

| Area | Current | New |
|---|---|---|
| Workout logging | Manual: exercise, sets, reps, weight | Schedule-driven: block checklist confirm |
| Diet | Not in app | New: meal blocks, diet days, weekly schedule |
| Measurements | Ad hoc metric logging | Weekly structured check-in |
| Social | Not in app | 90-day grid, global sprints, leaderboard |
| Home screen | Two big log buttons | Today's plan + single confirm flow |
| History screen | List of logged workouts/metrics | 90-day colour grid |
| Manage screen | CRUD exercise list | Schedule builder |

---

## Open Questions

None. All decisions resolved. ✅
