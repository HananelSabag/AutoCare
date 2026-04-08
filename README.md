# AutoCare 🚗

A personal car maintenance tracker for Android — built as a portfolio-quality app that I actually use on my own cars.

Hebrew-first, RTL-ready, Material You design with full Light / Dark / System theme support.

---

## Features

**Car Management**
- Add multiple cars with make/model dropdowns (35+ brands, 150+ models), photo, license plate, color picker, and expiry dates
- Car profile with hero photo, status banner, quick stats, and navigation to all sub-sections
- Full edit and delete with cascade cleanup of all related data

**Maintenance History**
- Log every service, repair, or wear item (tires, brakes, battery)
- Attach a receipt photo to any record — works as a digital car logbook
- Cost tracking with running totals and yearly statistics

**Test (MOT) History**
- Pass/fail records with optional certificate photo
- Quick access from car profile

**Smart Reminders**
- Per-car reminders for: test (MOT) expiry, compulsory insurance expiry, periodic service
- Escalation schedule: first alert at 60 days → 30 days → weekly → twice daily in the last week
- Test expiry reminder includes a prompt to bring the insurance certificate
- All reminders individually toggleable with custom lead-time

**Documents**
- Store photos of compulsory insurance and vehicle license per car

**Export**
- PDF report per car: summary, maintenance table with receipt indicators, test history, embedded receipt images
- JSON full backup of all data (all cars, records, reminders, documents)

**Settings**
- Theme: System / Light / Dark (persisted across sessions)
- Language: System / עברית / English (live switching, no restart needed)

---

## Tech Stack

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 + Material You (dynamic color) |
| Architecture | MVVM + Clean Architecture (data / domain / presentation) |
| Database | Room (with type converters) |
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
│   ├── components/     # Reusable composables (CarCard, etc.)
│   ├── navigation/     # Nav graph + Screen sealed class
│   ├── screens/        # One package per screen
│   └── theme/          # Material 3 theme, colors, typography
├── notifications/      # WorkManager worker + notification helper
├── export/             # PDF and JSON exporters
├── di/                 # Hilt modules
└── util/               # Extensions, status logic, car data
```

Clean Architecture layers are kept strict — the presentation layer never imports Room entities directly from the data layer *(with the pragmatic exception that entities serve as domain models to keep the project lean — no mapper boilerplate)*.

---

## Screenshots

*Coming soon*

---

## Getting Started

1. Clone the repo
2. Open in Android Studio Hedgehog or later
3. Run on a device or emulator (API 26+)

No API keys or external services required — fully local, fully offline.

---

## Status

Active development. Core features are complete and in daily use.

**Roadmap:**
- Excel / CSV export
- Statistics deep-dive (cost per km, spending trends)
- Proper Room migrations before public release

---

## Author

**Hananel Sabag** — Android developer

[LinkedIn](https://www.linkedin.com/in/hananel-sabag) · [GitHub](https://github.com/HanSab)
