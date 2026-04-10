# AutoCare

A personal car maintenance tracker for Android — built as a portfolio-quality app that I actually use on my own cars.

Hebrew-first, RTL-ready, Material You design with full Light / Dark / System theme support.

---

## Download

**[AutoCare.apk](AutoCare.apk)** — debug build, signed, ready to sideload (Android 8.0+)

> Enable *Install from unknown sources* in your device settings before installing.

---

## Features

### Car Management
- Add multiple cars with make/model dropdowns (35+ brands, 150+ models), photo, color picker, license plate, year, and expiry dates
- Full-bleed photo card catalog with HorizontalPager — peek-based swipe between cars
- Long-press enters **edit mode**: in-place left/right arrows to reorder, delete button, scroll locked — no bottom sheet
- Car profile with hero photo, transparent top bar, status banner, action tiles, and stats
- Full edit and delete with cascade cleanup of all related records and reminders

### Maintenance History
- Three record types: **Maintenance** (periodic service, km required), **Repair** (one-off fixes, km optional), **Wear** (tires, brakes, battery, wipers, km optional)
- Attach a receipt or part photo to any record
- Quick-fill suggestion chips per record type
- Cost tracking with running totals and per-type breakdown

### Smart Reminders
- Per-car reminders for: test (MOT) expiry, insurance expiry, and periodic service date
- Three notification windows: **60 days** / **30 days** / **7 days** before expiry
- Escalating schedule per window — e.g. 60-day window fires at: 60 → 30 → 23 → 16 → 9 days, then daily in the last 7
- Last 7 days always active regardless of window choice
- Live **"next alert" date** shown per reminder based on actual expiry + selected window
- Each reminder individually toggleable per car

### Statistics
- Cost breakdown by record type with a Canvas bar chart
- Total spend, record count, and average cost per service
- Only shown when at least 2 maintenance records exist

### Documents
- Store test certificate and insurance documents per car (image or PDF) with expiry date
- In-app photo framing: pinch-to-zoom, pan with edge clamping, rule-of-thirds grid, reset button
- Full document history with active/archived badges

### Export & Backup
- **PDF report** per car — hero photo, car details, full maintenance table with receipt photos
- **JSON backup** — full export of all cars and records; import restores everything by car ID

### Settings
- Theme: System / Light / Dark (persisted via DataStore)
- Language: System / עברית / English (live switching via AppCompatDelegate)
- Notification permission management with a **test notification** button to verify delivery

---

## Screenshots

*Coming soon*

---

## Tech Stack

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 + Material You (dynamic color) |
| Architecture | MVVM + Clean Architecture (data / domain / presentation) |
| Database | Room v7 — real migrations, data safe on upgrade |
| DI | Hilt |
| Navigation | Navigation Compose |
| Preferences | DataStore |
| Background | WorkManager (12h periodic reminder check) |
| Images | Coil 3 |
| Locale | AppCompatDelegate (live language switching) |
| Min SDK | 26 (Android 8.0) |
| Compile SDK | 35 |

---

## Architecture

```
com.hananelsabag.autocare/
├── data/
│   ├── local/          # Room DB, DAOs, entities, type converters
│   ├── preferences/    # DataStore wrappers
│   └── repository/     # Repository implementations
├── domain/
│   └── repository/     # Repository interfaces
├── presentation/
│   ├── components/     # Reusable composables (CarPager, ImageCropSheet, …)
│   ├── navigation/     # Nav graph + Screen sealed class
│   ├── screens/        # One package per screen
│   └── theme/          # Material 3 theme, colors, typography
├── notifications/      # WorkManager worker + notification helper
├── export/             # PDF and JSON exporters + JSON importer
├── di/                 # Hilt modules
└── util/               # Extensions, status logic, car data, form utils
```

Clean Architecture layers are kept strict — the presentation layer never imports Room entities directly from the data layer *(with the pragmatic exception that entities serve as domain models to keep the project lean — no mapper boilerplate)*.

---

## Getting Started

1. Clone the repo
2. Open in Android Studio Hedgehog or later
3. Run on a device or emulator (API 26+)

No API keys or external services required — fully local, fully offline.

---

## Author

**Hananel Sabag** — Android developer

[LinkedIn](https://www.linkedin.com/in/hananel-sabag) · [GitHub](https://github.com/HananelSabag)
