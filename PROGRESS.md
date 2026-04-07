# AutoCare — Progress Tracker

> **Purpose:** Cross-session continuity. Read this alongside `CLAUDE.md` and `PROJECT_SPEC.md` at the start of any new session. This is the living record of what was built, what decisions were made, and what comes next.

---

## Vision

A portfolio-quality Android app for tracking personal car maintenance. Hebrew-first, RTL, Material 3 with Material You. Built by Hananel Sabag.

**Not a feature dump.** Every screen earns its place. Data entry is kept minimal — users fill in what they have, not what we demand.

---

## What We Built (Session by Session)

### Phase 1 — Foundation & Infrastructure
- Project setup: Kotlin + Compose + Hilt + Room + DataStore + WorkManager + Coil + Navigation
- Clean Architecture: `data / domain / presentation` layers
- Hilt DI wired end-to-end (`AppDatabase`, `CarRepository`, modules)
- Material 3 + Material You (dynamic color) with full Light/Dark/System theme support
- **RTL forced globally** via `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` in Theme.kt
- Status color tokens: `StatusGreen`, `StatusYellow`, `StatusRed` (+ container variants) in Color.kt
- `StatusUtils.kt`: `getStatusLevel()`, `toFormattedDate()`, `daysFromNow()`, `toEpochMilli()`
- Custom launcher icon (created via Android Studio Asset Studio)
- Splash screen: logo + "Hananel Sabag | חננאל סבג" + version 1.0 (1.8s delay)
- `AppDatabase` version 2 with TypeConverters (`RecordType`, `ReminderType`)

### Phase 2 — Car Management
- `Car` entity: make, model, year, licensePlate, color?, photoUri?, currentKm?, testExpiryDate?, insuranceExpiryDate?, comprehensiveInsuranceExpiryDate?, notes?
- `CarDao`, `CarRepository`, `CarRepositoryImpl`
- `CarsScreen`: LazyColumn of `CarCard` items + FAB to add car
- `CarCard`: photo (AsyncImage/Coil) or placeholder icon, license plate badge, test/insurance status chips
- `AddCarSheetContent` (ModalBottomSheet): photo picker with persistable URI, required fields (make/model/year/plate), optional fields, date pickers (DatePickerDialog), notes. Shared for add and edit modes.
- `AddCarViewModel`: form state, validation (`FieldError.Required`, `FieldError.InvalidYear`), `lastSavedCarId` flow for post-save reminder prompt

### Phase 3 — Car Profile
- `CarProfileScreen`: full screen with TopAppBar (back, edit, delete buttons)
- `CarHeroSection`: 220dp photo with gradient or primaryContainer placeholder
- `StatusBanner`: card colored by worst status (GREEN/YELLOW/RED/EXPIRED/UNKNOWN), one row per document
- `QuickStatsRow`: km, test date, color chips
- Action buttons: maintenance history, test history (coming soon)
- Edit → ModalBottomSheet reusing `AddCarSheetContent`
- Delete → AlertDialog (only place AlertDialog is used per CLAUDE.md)
- `CarProfileViewModel`: `_carId` flow → `flatMapLatest` → `car` StateFlow + `deleteCar()` + `updateCar()`

### Phase 4 — Maintenance History
- `MaintenanceRecord` entity: id, carId (FK → Car with CASCADE), type (RecordType enum), date, description, km?, costAmount?, notes?, createdAt
- `RecordType` enum: `MAINTENANCE` (km required), `REPAIR` (km optional), `WEAR` (km optional)
- `MaintenanceRecordDao`, `MaintenanceRecordRepository`, `MaintenanceRecordRepositoryImpl`
- `MaintenanceHistoryViewModel`: `_carId` flow → records StateFlow + CRUD
- `AddMaintenanceRecordSheet` (ModalBottomSheet): type selector (FilterChip), date picker (always required), description, km (required for MAINTENANCE only), cost (optional), notes (optional). Local state — no separate ViewModel.
- `MaintenanceHistoryScreen`: LazyColumn of `MaintenanceRecordCard` items + FAB + tap → `RecordDetailSheet` → edit / delete (AlertDialog)
- Record detail shows only fields that were filled in (km, cost, notes hidden if null)

### Phase 5 — Reminders
- `Reminder` entity: id, carId (FK → Car with CASCADE), type (ReminderType enum), enabled, daysBeforeExpiry (default 14), createdAt
- `ReminderType` enum: `TEST_EXPIRY`, `INSURANCE_COMPULSORY_EXPIRY`, `INSURANCE_COMPREHENSIVE_EXPIRY`, `SERVICE_DATE`
- `ReminderDao`, `ReminderRepository`, `ReminderRepositoryImpl`
- `RemindersScreen`: car list (tappable) → navigate to per-car reminders. Empty state if no cars.
- `CarRemindersScreen`: per-car reminder config — shows only applicable types (based on what dates are filled in the car), Switch per type + days-before field, Save button writes to DB
- `CarRemindersViewModel`: loads car + reminders, `enableDefaultReminders()` for defaults (14 days before each filled-in date), `saveReminders()` for full replace
- **Post-car-creation reminder prompt**: after adding a car, `CarsScreen` shows a ModalBottomSheet offering "Enable default reminders" (14 days before test + insurance) or "Not now"

### Phase 6 — Settings
- `AppPreferencesDataStore`: DataStore wrapper for persistent app preferences (`theme_mode`)
- `SettingsViewModel`: reads/writes `ThemeMode`, activity-scoped via `by viewModels()` in `MainActivity`
- **Theme persists across sessions**: MainActivity collects `themeMode` → passes to `AutoCareTheme`
- `SettingsScreen`:
  - **Theme section**: FilterChip row — System / בהיר / כהה
  - **Language section**: shows "עברית", note that English is coming soon (mechanism is ready — just add `strings.xml` in `values-en/`)
  - **Notifications section**: checks `POST_NOTIFICATIONS` permission (API 33+). If disabled → error-colored button to enable via `rememberLauncherForActivityResult`. If enabled → shows "פעיל ומחובר ✓"
  - **About section**: version name (from `BuildConfig.VERSION_NAME`), developer name "חננאל סבג"

### Architecture Decisions Made
- **No center FAB**: Removed. Each screen owns its FAB (CarsScreen adds cars, MaintenanceHistoryScreen adds records). Cleaner M3 design.
- **No mapper layers**: Room entities used directly as domain models throughout.
- **Reminders always tied to carId**: No standalone reminders. The Reminders tab shows a car list — you tap a car to manage its reminders.
- **No km tracking outside records**: Km is only entered when logging a maintenance record.
- **DB version 2**: `fallbackToDestructiveMigration()` — dev builds only. Will need proper migration before any real user data.

---

## Current State

**What works (compile-verified):**
- Full car CRUD (add, view, edit, delete)
- Car profile with status banner
- Maintenance history with full CRUD (add, tap to view, edit, delete)
- Reminders tab with per-car management
- Post-car-creation reminder prompt
- Settings: theme switching (persists), notification permission check, version info
- Splash screen
- Navigation: Cars → Car Profile → Maintenance History / Car Reminders

**Placeholder / Coming Soon:**
- Test history (button exists in CarProfileScreen, opens "בקרוב" sheet)
- Export: PDF, Excel, JSON (no implementation yet)

**What's Live:**
- WorkManager daily notification check (`ReminderCheckWorker`) — fires notifications when within `daysBeforeExpiry` of expiry
- Statistics card in CarProfileScreen — total records, last service, spent this year, avg cost (shown from 1 record)
- Documents screen — store photo per document type (insurance compulsory, comprehensive, vehicle license); accessible from car profile
- English translation (`values-en/strings.xml`) — full translation; activates automatically when device is in English

---

## What's Next (Priority Order)

1. **Test History** — simple list of past test dates + pass/fail per car (new entity)
2. **Export** — PDF (beautiful with logo + data), Excel, JSON backup/restore
3. **In-app language switcher** — values-en/ exists; add AppCompatDelegate.setApplicationLocales() to SettingsScreen language section
4. **Proper DB migrations** — replace `fallbackToDestructiveMigration()` before release (DB currently at version 3)
5. **Statistics deep-dive** — cost per km chart, most common repair type, spending trend

---

## Known Improvement Areas

- `CarRemindersScreen` uses a complex `remember(savedReminders, applicableTypes)` pattern that rebuilds state on every recompose. Consider moving reminder edit state into `CarRemindersViewModel` for cleaner separation.
- `AddMaintenanceRecordSheet` uses local state — fine for now, but if the app grows (e.g., multi-step flows), a dedicated ViewModel would be cleaner.
- The `CarsScreen` instantiates three ViewModels (`CarsViewModel`, `AddCarViewModel`, `CarRemindersViewModel`). `CarRemindersViewModel` is only used for `enableDefaultReminders()` — consider extracting this to a simpler use-case or moving it into `AddCarViewModel`.
- `STATUS_YELLOW` threshold: currently 8–30 days. Could be user-configurable per reminder type in a future version.

---

## Tech Stack (Current)

| Library | Version | Use |
|---|---|---|
| Kotlin | 2.0.21 | Language |
| Compose BOM | 2024.12.01 | UI |
| Hilt | 2.51.1 | DI |
| Room | 2.6.1 | Local DB |
| Navigation Compose | 2.8.5 | Navigation |
| DataStore | 1.1.1 | Preferences |
| WorkManager | 2.9.1 | Background tasks |
| Coil 3 | 3.0.4 | Image loading |
| Coroutines | 1.9.0 | Async |
| KSP | 2.0.21-1.0.28 | Annotation processing |
| compileSdk | 35 | Stable (no betas) |
| minSdk | 26 | Android 8.0+ |
