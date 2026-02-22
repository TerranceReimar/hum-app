# 🏋️ GymTracker — High-Speed Logging Ecosystem

A Kotlin Jetpack Compose Android app + FastAPI Python backend for lightning-fast gym and body metric logging on your own Raspberry Pi.

---

## Architecture Overview

```
Android Phone  ──Retrofit──▶  FastAPI (Pi)  ──SQLite──▶  gymtracker.db
    (LAN)                    port 8000
```

---

## 🥧 Backend — Raspberry Pi Setup

### Option A: Docker (Recommended)

```bash
# 1. Clone / copy the backend/ folder to your Pi
scp -r backend/ pi@<PI_IP>:~/gymtracker/

# 2. SSH in
ssh pi@<PI_IP>
cd ~/gymtracker

# 3. Start
docker-compose up -d

# 4. Check logs
docker-compose logs -f
```

The API is now live at `http://<PI_IP>:8000`  
Interactive docs: `http://<PI_IP>:8000/docs`

### Option B: Direct Python (no Docker)

```bash
# On the Pi:
cd backend/
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# Init DB and run
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

## 📱 Android App — Setup & Build

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17+
- Android SDK 34

### Build Steps

```bash
# 1. Open android/ folder in Android Studio
# 2. Let Gradle sync finish
# 3. Connect device or start emulator
# 4. Run the app (Shift+F10)
```

### First Launch
1. Tap **Settings** (bottom nav)
2. Enter your Pi's IP: `http://192.168.1.100:8000`
3. Tap **Test** to verify connection
4. Tap **Save**
5. Go back to **Log** and start tracking!

---

## 📁 Project Structure

```
gym-tracker/
├── backend/
│   ├── main.py              # FastAPI app — all endpoints + SQLite init
│   ├── requirements.txt     # fastapi, uvicorn, pydantic
│   ├── Dockerfile
│   └── docker-compose.yml
│
└── android/
    ├── build.gradle.kts     # Root build
    ├── settings.gradle.kts
    └── app/
        ├── build.gradle.kts # App deps (Compose, Retrofit, Vico charts…)
        └── src/main/
            ├── AndroidManifest.xml
            └── java/com/gymtracker/
                ├── MainActivity.kt              # Nav scaffold + Snackbar events
                ├── data/
                │   ├── api/
                │   │   ├── GymApiService.kt     # Retrofit interface
                │   │   └── RetrofitClient.kt    # Singleton, dynamic base URL
                │   ├── models/Models.kt         # Data classes
                │   └── SettingsRepository.kt    # DataStore for base URL
                └── ui/
                    ├── MainViewModel.kt         # All API calls + StateFlows
                    ├── theme/Theme.kt           # Dark gym palette (neon lime)
                    ├── home/HomeScreen.kt       # Quick Log buttons + dialogs
                    ├── history/HistoryScreen.kt # Cards + mini line chart
                    ├── manage/ManageScreen.kt   # CRUD exercises & measurements
                    └── settings/SettingsScreen.kt # URL config + connection test
```

---

## 🗄️ Database Schema

```sql
-- Exercises available for selection
exercise_list (id, name, category)

-- Logged workout sessions
workouts (id, timestamp, exercise_name, sets, reps, weight, tempo)

-- Body metrics log
user_metrics (id, timestamp, metric_type, value, notes)

-- Configurable measurement types
measurement_types (id, name)
```

---

## 🔌 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Connection test |
| GET | `/exercises` | List all exercises |
| POST | `/exercises` | Create exercise `{name, category}` |
| PUT | `/exercises/{id}` | Update exercise |
| DELETE | `/exercises/{id}` | Delete exercise |
| GET | `/measurement-types` | List measurement types |
| POST | `/measurement-types` | Add type `{name}` |
| DELETE | `/measurement-types/{id}` | Delete type |
| GET | `/workouts?limit=100` | List workout logs |
| POST | `/workouts` | Log workout `{exercise_name, sets, reps, weight, tempo}` |
| DELETE | `/workouts/{id}` | Delete log |
| GET | `/metrics?limit=200&metric_type=Weight` | List metrics |
| POST | `/metrics` | Log metric `{metric_type, value, notes}` |
| DELETE | `/metrics/{id}` | Delete metric |

---

## 🎨 UI Feature Highlights

| Feature | Implementation |
|---------|---------------|
| Dark gym theme | Electric lime (#CCFF00) + teal (#4ECDC4) accents |
| High-speed workout log | Searchable dropdown → fill Sets/Reps/Weight/Tempo → 1 tap |
| Bulk metric logging | All metrics in one dialog, logs each non-empty field |
| History cards | Color-coded accent bars, stat chips, swipe-delete |
| Progress chart | Canvas-drawn line chart for Weight & Waist trends |
| Category colors | Push=orange, Pull=teal, Legs=yellow |
| Connection feedback | Snackbar on every sync success/failure |
| Configurable URL | DataStore-persisted base URL, live connection test |

---

## 🔧 Troubleshooting

**"Network error" on Android:**
- Ensure both phone and Pi are on the same WiFi
- Check `usesCleartextTraffic="true"` is in AndroidManifest (HTTP to local IP)
- Verify Pi firewall: `sudo ufw allow 8000`

**Pi IP keeps changing:**
- Set a static IP in your router's DHCP settings for the Pi's MAC address

**Docker permission denied:**
```bash
sudo usermod -aG docker pi && newgrp docker
```

**Data not showing after log:**
- Pull-to-refresh (tap the ↻ icon in History header)

---

## 📈 Extending

- **Add OAuth**: FastAPI supports OAuth2 with JWT — add `python-jose` and `passlib`
- **Charts**: Swap Canvas chart for [Vico](https://patrykandpatrick.com/vico/) library (already in deps)
- **Backup**: `docker-compose.yml` mounts `./data` — just back up that folder
- **HTTPS**: Put Nginx + Let's Encrypt in front of Uvicorn for remote access
