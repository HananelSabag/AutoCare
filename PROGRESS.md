# AutoCare — Progress Tracker

> **Purpose:** Cross-session continuity. Read this alongside `CLAUDE.md` at the start of any new session. This is the living record of what was built, what decisions were made, and what comes next.

---

## Vision

A portfolio-quality Android app for tracking personal car maintenance. Hebrew-first, RTL, Material 3 with Material You. Built by Hananel Sabag.

**Not a feature dump.** Every screen earns its place. Data entry is kept minimal — users fill in what they have, not what we demand.

---

## Tech Stack

| Library | Version | Use |
|---|---|---|
| Kotlin | 2.0.21 | Language |
| Compose BOM | 2024.12.01 | UI |
| Hilt | 2.51.1 | DI |
| Room | 2.6.1 | Local DB (v7) |
| Navigation Compose | 2.8.5 | Navigation |
| DataStore | 1.1.1 | Preferences |
| WorkManager | 2.9.1 | Background tasks |
| Coil 3 | 3.0.4 | Image loading |
| AppCompat | 1.7.0 | Language switching |
| Coroutines | 1.9.0 | Async |
| KSP | 2.0.21-1.0.28 | Annotation processing |
| compileSdk | 35 | |
| minSdk | 26 | Android 8.0+ |

---

## Architecture Decisions (Settled — Do Not Re-Open)

- **MVVM + Clean Architecture**: data / domain / presentation layers
- **No mapper layers**: Room entities used directly as domain models
- **No center FAB**: each screen owns its FAB
- **No km tracking outside maintenance records**
- **No fuel tracking in v1**
- **No Excel export** — dropped entirely, not "coming soon"
- **No comprehensive insurance** — only טסט + ביטוח חובה
- **No TestHistoryScreen UI** — `TestRecord` entity kept for export only
- **ModalBottomSheet for all input**, AlertDialog for destructive confirmations only
- **Reminders always tied to carId** — no standalone reminders
- **RTL conditional**: Hebrew → RTL, English → LTR (via `LocalConfiguration.current.locales[0]`)
- **3 record types**: `MAINTENANCE` (km required), `REPAIR` (km optional), `WEAR` (km optional)
- **DB migrations**: real migrations for all versions 1→7 (no `fallbackToDestructiveMigration`)

---

## What's Built — Full Feature List

### Infrastructure
- Project: Kotlin + Compose + Hilt + Room + DataStore + WorkManager + Coil + Navigation
- Clean Architecture: `data / domain / presentation` layers wired end-to-end
- Material 3 + Material You (dynamic color), Light / Dark / System theme — all three working
- Custom launcher icon + splash screen (logo + "Hananel Sabag | חננאל סבג" + version, 1.8s)
- Status color tokens: `StatusGreen`, `StatusYellow`, `StatusRed` in `Color.kt`
- `StatusUtils.kt`: `getStatusLevel()`, `toFormattedDate()`, `daysFromNow()`, `toEpochMilli()`
- `CarColorUtils.kt`: `carColorToComposeColor(name)` — Hebrew/English → Compose Color
- `FormInputUtils.kt`: `ThousandsVisualTransformation` (visual commas on number fields), `LicensePlateVisualTransformation`
- `CarMakeData.kt`: 35+ makes, 150+ models with per-make suggestions

### DB Schema (v7)
Tables: `cars`, `maintenance_records`, `reminders`, `test_records`, `vehicle_records`
Real migrations covering v1→v7, v2→v7, ..., v6→v7 — data safe on reinstall/upgrade

### Cars Screen (`CarsScreen`)
- **HorizontalPager card catalog** (`CarPager.kt`) — full-bleed photo cards with gradient scrim
- **AddCarPagerCard** — "add new car" shown as the last card in the pager
- **Empty state** when no cars exist
- **Long-press context menu** on any car card: "העבר לראשון" (move to front) + "מחק"
- **Car reorder**: `CarsViewModel` exposes `moveLeft()` / `moveRight()` + `displayOrder` persisted to DB
- **Scroll-to-car** after reorder via `pagerState.animateScrollToPage()`
- **AddCarSheet** (`ModalBottomSheet`): ExposedDropdownMenuBox for make/model, `LicensePlateVisualTransformation` (cursor-safe RTL formatting), `ThousandsVisualTransformation` on km, 15-color circle picker, photo picker (180dp), year chips (animated), `LocalSoftwareKeyboardController` to hide keyboard on dropdown expand
- **Discard confirmation** AlertDialog if sheet dismissed with unsaved data (`hasUnsavedData()`)
- **Two-step post-add flow**: step 1 = enable reminders prompt, step 2 = add first service prompt

### Car Profile (`CarProfileScreen`)
- Full-bleed hero (248dp): photo behind transparent TopAppBar, top+bottom scrim gradients
- Edit/Delete icon pill in semi-transparent surface for contrast on any background
- **StatusBanner**: 3 inline mini-cards (טסט, ביטוח, שירות) with `VerticalDivider` separators, each colored by own status level, expired items get 2dp `StatusRed` border, day-count badge (number + "ימים" / "פג!" / "היום" / "?")
- **Action tile grid**: 2×2 grid of 100dp cards (Maintenance = primaryContainer, others = surfaceContainerLow)
- **Stats CTA**: if < 2 records → nudge card "הוסף את הטיפול הראשון"; if ≥ 2 records → `CarStatsSheet` bottom sheet with Canvas bar chart, cost breakdown by type, record count, average cost
- Notes section wrapped in Card
- Placeholder hero: gradient tinted by car's actual color via `CarColorUtils`
- 3-tile action row: Maintenance, Documents, Reminders

### Maintenance History (`MaintenanceHistoryScreen`)
- Full CRUD for maintenance records
- Left accent bar colored by type (primary/error/tertiary)
- Type icon (Build/CarRepair/Autorenew), type badge chip, cost shown inline
- `RecordDetailSheet`: full details, receipt photo thumbnail (180dp), edit/delete
- `AddMaintenanceRecordSheet`: dynamic description placeholder per type, `SuggestionChip` quick-fill row (colors match record type), char counter (200 max), `ThousandsVisualTransformation` on km and cost, photo picker for receipt

### Documents (`CarDocumentsScreen`, `VehicleRecordHistoryScreen`)
- `VehicleRecord` entity: type (TEST/INSURANCE), expiryDate, fileUri?, isActive, createdAt
- One card per type with editable expiry + optional file attach (image or PDF)
- Type-specific icon badges (Security/DirectionsCar), full-width 180dp thumbnail
- **History screen** (`VehicleRecordHistoryScreen`): full list with active/archived badges
- `ImageCropSheet`: pinch-to-zoom crop UI for photo picking

### Reminders (`RemindersScreen`, `CarRemindersScreen`)
- **RemindersScreen**: full redesign — `RemindersDashboardViewModel` drives it
  - Car picker: 2-column grid, expand/collapse (shows 4 initially, "הצג עוד" button)
  - Selected car's `CarRemindersScreen` embedded inline below picker (no separate nav)
- **CarRemindersScreen**: always shows all 3 reminder types (TEST, INSURANCE, SERVICE)
  - Per-card urgency-colored icon badge, countdown chip "עוד X ימים · DD/MM/YYYY"
  - Missing date warning chip (amber) if date not set / no service records
  - Animated expand/collapse for schedule info + custom days-before field
  - Urgency border on card when status is yellow/red
- `CarRemindersViewModel`: `formState` initialized once from DB, `lastMaintenanceDate` for SERVICE countdown, `updateEnabled()` / `updateWindow()` / `saveReminders()`. `nextFireDateMs(expiryMs, window)` computes exact date of next notification by mirroring worker logic.
- `ReminderCheckWorker`: escalation schedule — 60d, 30d, weekly (23/16/9d), then every run ≤7d (≈twice daily). `SERVICE_DATE` = last MAINTENANCE + 365d. Stable notification IDs: `carId * 10 + type.ordinal`. 12-hour periodic interval.
- **Reminder UX**: Chips show "60 יום / 30 יום / 7 ימים" (the start of the escalation window). Expanded section shows human-readable escalation description + "ההתראה הבאה: DD/MM/YYYY" calculated in real time. No free-text number input — user picks only from the 3 defined windows.

### Settings (`SettingsScreen`)
- Theme: System / Light / Dark FilterChips (persisted via DataStore, survives app restart)
- Language: מערכת / עברית / English — calls `AppCompatDelegate.setApplicationLocales()`, instant restart
- Notifications: shows current status, button to enable if denied (runtime permission)
- **Export section** (fully functional):
  - **PDF**: single-car export; if multiple cars → car picker BottomSheet. Saved to Downloads (API 29+) or share intent fallback. Snackbar with "פתח" action.
  - **JSON backup**: exports all data to Downloads + share option. Snackbar with "שתף" action.
  - **JSON import**: file picker (`GetContent`), restores all cars + records. Success snackbar with count.
  - No Excel — removed entirely.
- About: version (from `BuildConfig`), developer name, tappable with ripple

### Notifications
- `NotificationHelper`: creates notification channel, posts notifications with car name + days remaining
- `ReminderCheckWorker`: checks all enabled reminders on 12h schedule, posts escalating notifications

### Export / Import
- `PdfExporter`: full car report — hero photo, car details, maintenance records with photos, vehicle records, status summary
- `JsonExporter`: full backup of all cars + all record types as JSON
- `JsonImporter`: restores from JSON backup, maps all relationships by carId

---

## Current State

**Everything compile-verified and pushed to `origin/main`.**

**Fully working:**
- Car CRUD + reorder
- Car profile with hero, status banner, stats, all action tiles
- Maintenance history (full CRUD + photos)
- Documents (per-type cards + history)
- Reminders (dashboard + per-car config + WorkManager notifications)
- Settings (theme, language, notifications, PDF export, JSON backup/restore)
- Splash screen

**Intentionally out of scope (v1):**
- Excel export — dropped
- Fuel tracking — dropped
- VIN scanner / auto-lookup — dropped
- Test History UI — entity exists for export only
- Comprehensive insurance — removed entirely

---

## What's Next (Priority Order)

1. **Statistics screen** — full screen with cost-per-km chart, spending trend, most common repair type. Shown when ≥ 2 records (per spec). `CarStatsSheet` exists as a starting point.
2. **OneMoreServiceCtaCard polish** — LinearProgressIndicator showing km progress toward next service
3. **QA / real-device testing pass** — test all flows end-to-end on a real device before calling v1 done

---

## Known Improvement Areas

- `CarsScreen` instantiates three ViewModels — `CarRemindersViewModel` only used for `enableDefaultReminders()`; could be scoped better.
- `STATUS_YELLOW` threshold hardcoded at 8–30 days — could be user-configurable per type (outer window already is configurable).
- `AddMaintenanceRecordSheet` uses local state — fine for now.
