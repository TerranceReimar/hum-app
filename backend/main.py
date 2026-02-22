from contextlib import asynccontextmanager
from datetime import datetime

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from database import init_db
from routers import profiles, exercises, measurements, workout_schedule, diet_schedule, schedule, daily_logs, sprints


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    yield


app = FastAPI(title="Hum API", version="2.0.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(profiles.router)
app.include_router(exercises.router)
app.include_router(measurements.router)
app.include_router(workout_schedule.router)
app.include_router(diet_schedule.router)
app.include_router(schedule.router)
app.include_router(daily_logs.router)
app.include_router(sprints.router)


@app.get("/health")
def health():
    return {"status": "ok", "timestamp": datetime.utcnow().isoformat()}
