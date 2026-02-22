# Hum — Fitness Tracking on Your Own Pi

A Kotlin Jetpack Compose Android app + FastAPI Python backend for lightning-fast gym and body metric logging on your own Raspberry Pi. Supports multiple named profiles — no passwords, just pick your name and go.

---

## Architecture Overview

```
Android Phone  ──Retrofit HTTP──▶  FastAPI (Pi :8000)  ──SQLite──▶  gymtracker.db
    (LAN only)
```

---

## Raspberry Pi — First-Time Setup

### 1. Install Docker on the Pi

```bash
# SSH into the Pi
ssh pi@<PI_IP>

# Install Docker (official convenience script)
curl -fsSL https://get.docker.com | sh

# Add pi user to docker group (avoids needing sudo)
sudo usermod -aG docker pi
newgrp docker

# Verify
docker --version
```

### 2. Deploy the Backend

```bash
# From your dev machine — copy the backend folder to the Pi
scp -r backend/ pi@<PI_IP>:~/gymtracker/

# SSH back in
ssh pi@<PI_IP>
cd ~/gymtracker

# Build and start (runs in background, restarts on reboot)
docker-compose up -d --build

# Watch the logs to confirm startup
docker-compose logs -f
```

You should see:
```
gymtracker_api  | INFO:     Application startup complete.
```

The API is now live at `http://<PI_IP>:8000`
Interactive docs (Swagger UI): `http://<PI_IP>:8000/docs`

### 3. Find Your Pi's IP Address

```bash
# Run on the Pi
hostname -I
```

Or find it from your router's DHCP client list. **Set a static/reserved IP** in your router so it never changes (bind it to the Pi's MAC address).

### 4. Open the Firewall (if UFW is enabled)

```bash
sudo ufw allow 8000/tcp
sudo ufw status
```

### 5. Verify the Server is Working

```bash
# Quick health check from any machine on your LAN
curl http://<PI_IP>:8000/health
# → {"status":"ok","timestamp":"..."}

# Confirm profiles endpoint exists
curl http://<PI_IP>:8000/profiles
# → []
```

---

## Option B: Direct Python (no Docker)

```bash
cd ~/gymtracker/backend
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python main.py
```

For autostart on boot, create a systemd service:

```ini
# /etc/systemd/system/gymtracker.service
[Unit]
Description=GymTracker API
After=network.target

[Service]
WorkingDirectory=/home/pi/gymtracker/backend
ExecStart=/home/pi/gymtracker/backend/venv/bin/python main.py
Restart=always
User=pi

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable gymtracker
sudo systemctl start gymtracker
```

---

## Android App — Setup & Build

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17+
- Android SDK 34

### Build & Install

```bash
# Command line (device connected via USB with USB debugging on)
cd android
./gradlew installDebug

# Or open the android/ folder in Android Studio and press Shift+F10
```

### First Launch — Connecting to the Pi

The app opens to a **Profile Login screen**. No profiles exist yet, so:

1. The screen will show "No profiles found" if it can't reach the server.
2. Tap the gear icon or navigate to **Settings** from within the login screen.
3. Enter your Pi's URL: `http://192.168.1.100:8000` (use your actual IP).
4. Tap **Test** — you should see "Connected!".
5. Tap **Save**.
6. Go back to the Profile screen and tap **Refresh**.
7. Tap **Create New Profile**, enter a name, and tap **Create**.
8. You're in — the full 5-tab app loads.

### Subsequent Launches

The app remembers your selected profile. It restores automatically on startup as long as the server is reachable. If the server is unreachable, it stays on the login screen until you reconnect.

### Switching Profiles / Signing Out

- **Settings → Sign Out** clears the saved profile and returns to the login screen.
- On the login screen, tap any listed profile to switch to it.

---

## Project Structure

```
gym-tracker/
├── backend/
│   ├── main.py              # FastAPI app — all endpoints, SQLite schema init + migrations
│   ├── requirements.txt     # fastapi, uvicorn, pydantic
│   ├── Dockerfile
│   └── docker-compose.yml   # Mounts ./data for DB persistence, restart: unless-stopped
│
└── android/
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── app/
        ├── build.gradle.kts
        └── src/main/
            ├── AndroidManifest.xml      # usesCleartextTraffic=true for LAN HTTP
            └── java/com/gymtracker/
                ├── MainActivity.kt              # Conditional nav: login gate vs 5-tab scaffold
                ├── data/
                │   ├── api/
                │   │   ├── GymApiService.kt     # Retrofit interface (all endpoints)
                │   │   └── RetrofitClient.kt    # Singleton, dynamic base URL
                │   ├── models/Models.kt         # Data classes (Profile, Workout, Metric…)
                │   └── SettingsRepository.kt    # DataStore: base URL + active profile ID
                └── ui/
                    ├── MainViewModel.kt         # All API calls + StateFlows
                    ├── theme/Theme.kt           # Dark gym palette (neon lime)
                    ├── home/HomeScreen.kt       # Quick Log buttons + dialogs
                    ├── history/HistoryScreen.kt # Cards + mini line chart
                    ├── manage/ManageScreen.kt   # CRUD exercises & measurement types
                    ├── profile/
                    │   ├── ProfileLoginScreen.kt  # Login gate: list/create profiles
                    │   └── ProfileTabScreen.kt    # Profile tab: weight + body dimensions
                    └── settings/SettingsScreen.kt # URL config, connection test, sign out
```

---

## Database Schema

```sql
-- Named user profiles
profiles (
    id, name UNIQUE,
    starting_weight, current_weight, goal_weight,
    starting_waist,  current_waist,  goal_waist,
    starting_arm,    current_arm,    goal_arm,
    starting_chest,  current_chest,  goal_chest,
    starting_thigh,  current_thigh,  goal_thigh,
    starting_neck,   current_neck,   goal_neck,
    starting_hip,    current_hip,    goal_hip,
    created_at
)

-- Exercises available for selection
exercise_list (id, name, category)

-- Logged workout sessions (profile-scoped)
workouts (id, timestamp, exercise_name, sets, reps, weight, tempo, profile_id)

-- Body metrics log (profile-scoped)
user_metrics (id, timestamp, metric_type, value, notes, profile_id)

-- Configurable measurement types
measurement_types (id, name)
```

> `profile_id` columns are added via `ALTER TABLE` migration on first run — existing data is preserved with `profile_id = NULL`.

---

## API Endpoints

### Profiles

| Method | Endpoint | Body / Params | Description |
|--------|----------|---------------|-------------|
| GET | `/profiles` | — | List all profiles |
| POST | `/profiles` | `{name, starting_weight?, …}` | Create profile |
| GET | `/profiles/{id}` | — | Get single profile |
| PUT | `/profiles/{id}` | any profile fields (all optional) | Update profile |
| DELETE | `/profiles/{id}` | — | Delete profile |

### Exercises

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/exercises` | List all exercises |
| POST | `/exercises` | Create `{name, category}` |
| PUT | `/exercises/{id}` | Update |
| DELETE | `/exercises/{id}` | Delete |

### Measurement Types

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/measurement-types` | List types |
| POST | `/measurement-types` | Create `{name}` |
| DELETE | `/measurement-types/{id}` | Delete |

### Workouts

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/workouts?profile_id=1&limit=50` | List (filter by profile) |
| POST | `/workouts` | Log `{exercise_name, sets, reps, weight, tempo, profile_id?}` |
| DELETE | `/workouts/{id}` | Delete |

### Metrics

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/metrics?profile_id=1&metric_type=Weight&limit=100` | List (filter by profile/type) |
| POST | `/metrics` | Log `{metric_type, value, notes, profile_id?}` |
| DELETE | `/metrics/{id}` | Delete |

### Health

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Server heartbeat |

---

## UI Feature Highlights

| Feature | Detail |
|---------|--------|
| Profile login gate | Pick or create a profile on launch; remembered across sessions |
| Profile tab | Edit starting/current/goal weight and 6 body dimensions |
| Dark gym theme | Electric lime (#CCFF00) + teal accents |
| High-speed workout log | Searchable dropdown → Sets/Reps/Weight/Tempo → 1 tap |
| Bulk metric logging | All measurement types in one dialog |
| History cards | Color-coded, stat chips, swipe-delete |
| Progress chart | Line chart for Weight & Waist trends |
| Category colors | Push=orange, Pull=teal, Legs=yellow |
| Connection feedback | Snackbar on every sync success/failure |
| Configurable URL | DataStore-persisted, live test button |

---

## Troubleshooting

**"No profiles found" on login screen**
- The server URL may not be saved yet — go to Settings, enter `http://<PI_IP>:8000`, Test, and Save.
- Confirm the Pi server is running: `docker-compose ps` on the Pi.
- Make sure phone and Pi are on the same WiFi network.

**"Network error" on Android**
- Verify `usesCleartextTraffic="true"` is in `AndroidManifest.xml` (required for plain HTTP).
- Check Pi firewall: `sudo ufw allow 8000/tcp`.
- Test from a browser: `http://<PI_IP>:8000/health`.

**Pi IP keeps changing**
- Reserve a static IP in your router's DHCP settings using the Pi's MAC address (`ip link show eth0`).

**Docker permission denied**
```bash
sudo usermod -aG docker pi && newgrp docker
```

**Profile existed but now shows "not found" on launch**
- The app auto-clears a stale saved profile ID and returns to the login screen. Just re-select your profile.

**Data not appearing after a log**
- Tap the refresh icon in the History header to force a reload.

**DB backup**
```bash
# On Pi — the DB lives in the Docker volume mount
cp ~/gymtracker/data/gymtracker.db ~/gymtracker/data/gymtracker.db.bak
```

---

## Extending

- **HTTPS**: Put Nginx + Certbot in front of Uvicorn for remote (non-LAN) access.
- **Charts**: Vico library is already in the Gradle deps — swap the Canvas chart for richer graphs.
- **Backup**: The `docker-compose.yml` mounts `./data` — just back up that folder.
- **Auth**: FastAPI supports OAuth2 with JWT — add `python-jose` and `passlib`.
