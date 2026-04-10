# AutoCare 🚗

A personal car maintenance tracker for Android — built as a portfolio-quality app that I actually use on my own cars.

Hebrew-first, RTL-ready, Material You design with full Light / Dark / System theme support.

---

## Features

**Car Management**
- Add multiple cars with make/model dropdowns (35+ brands, 150+ models), photo, license plate, color picker, and expiry dates
- Car catalog with full-bleed photo cards, long-press to reorder or delete
- Car profile with hero photo, status banner, action tiles, and quick stats
- Full edit and delete with cascade cleanup of all related data

**Maintenance History**
- Log every service, repair, or wear item (tires, brakes, battery)
- Attach a receipt photo to any record — works as a digital car logbook
- Cost tracking with running totals and per-type breakdown

**Smart Reminders**
- Per-car reminders for: test (MOT) expiry, insurance expiry, periodic service
- Escalation schedule: first alert at 60 days → 30 days → weekly → twice daily in the last week
- All reminders individually toggleable with custom lead-time window

**Documents**
- Store test certificate and insurance documents per car (image or PDF)
- Full document history with active/archived status

**Export & Backup**
- PDF report per car with full details, photos, and maintenance table
- JSON full backup of all data — export and reimport to restore everything
- JSON import: restore all cars and records from a backup file

**Settings**
- Theme: System / Light / Dark (persisted across sessions)
- Language: System / עברית / English (live switching)
- Notification permission management

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
| Database | Room v7 (with full migrations — data safe on upgrade) |
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
│   ├── components/     # Reusable composables (CarCard, CarPager, etc.)
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

## Download

See [Releases](https://github.com/HananelSabag/AutoCare/releases) for the latest APK.

---

## Author

**Hananel Sabag** — Android developer

[LinkedIn](https://www.linkedin.com/in/hananel-sabag) · [GitHub](https://github.com/HananelSabag)
