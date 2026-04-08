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
- **RTL now conditional**: Hebrew → RTL, English → LTR (via `LocalConfiguration.current.locales[0]` in Theme.kt)
- Status color tokens: `StatusGreen`, `StatusYellow`, `StatusRed` (+ container variants) in Color.kt
- `StatusUtils.kt`: `getStatusLevel()`, `toFormattedDate()`, `daysFromNow()`, `toEpochMilli()`
- Custom launcher icon (Android Studio Asset Studio) — used throughout app
- Splash screen: app icon + "Hananel Sabag | חננאל סבג" + version 1.0 (1.8s delay)
- `AppDatabase` version 4 with TypeConverters (`RecordType`, `ReminderType`)

### Phase 2 — Car Management
- `Car` entity: make, model, year, licensePlate, color?, photoUri?, currentKm?, testExpiryDate?, insuranceExpiryDate?, comprehensiveInsuranceExpiryDate?, notes?
- `CarDao`, `CarRepository`, `CarRepositoryImpl`
- `CarsScreen`: LargeTopAppBar (collapsing) with car count subtitle, LazyColumn of `CarCard` items + FAB
- `CarCard`: left colored accent bar (green/yellow/red per worst status), bigger 76dp photo, color dot, premium layout
- **AddCarSheet**: `ExposedDropdownMenuBox` for Make (35+ brands) with filtered suggestions, Model with per-make suggestions (free text still allowed)
- `AddCarViewModel`: form state, validation (`FieldError.Required`, `FieldError.InvalidYear`), `lastSavedCarId` flow for post-save reminder prompt
- Empty state: large icon, descriptive subtitle, FilledTonalButton CTA
- **Notification permission**: asked on first launch via bottom sheet (one-time, DataStore flag)

### Phase 3 — Car Profile
- `CarProfileScreen`: full screen with TopAppBar (transparent overlay on hero)
- `CarHeroSection`: 240dp photo with gradient + make/model/plate overlay text; or gradient placeholder with car icon + name
- `StatusBanner`: card colored by worst status, each row has a matching icon (VerifiedUser/Security)
- `QuickStatsRow`: km, test date, color chips — with icons
- Action buttons: FilledTonalButton (maintenance) + OutlinedButton (test history — navigates to real screen), OutlinedButton (documents), all 52dp height
- Edit → ModalBottomSheet reusing `AddCarSheetContent`
- Delete → AlertDialog (only place AlertDialog is used)
- `CarProfileViewModel`: `_carId` flow → `flatMapLatest` → `car` StateFlow + `deleteCar()` + `updateCar()`

### Phase 4 — Maintenance History
- `MaintenanceRecord` entity: id, carId (FK → Car CASCADE), type (RecordType), date, description, km?, costAmount?, notes?, createdAt
- `RecordType` enum: `MAINTENANCE` (km required), `REPAIR` (km optional), `WEAR` (km optional)
- Record cards: left accent bar colored by type (primary/error/tertiary), type icon (Build/CarRepair/Autorenew), type badge chip, cost shown
- `RecordDetailSheet`: vertically scrollable, icons, edit/delete buttons
- FAB uses `primary` color (not primaryContainer) for stronger CTA feel

### Phase 5 — Reminders (Fully Reworked)
- `Reminder` entity: id, carId (FK → Car CASCADE), type (ReminderType), enabled, daysBeforeExpiry (default **60**), createdAt
- `ReminderType` enum: `TEST_EXPIRY`, `INSURANCE_COMPULSORY_EXPIRY`, `SERVICE_DATE`
- `RemindersScreen`: car list → navigate to per-car reminders
- **`CarRemindersScreen` (major overhaul)**:
  - Always shows all 3 reminder types (even if car dates not set)
  - Per-card urgency-colored icon badge (green/yellow/red via `StatusLevel`)
  - **Countdown chip** on every card: "עוד X ימים · DD/MM/YYYY" in matching status color
  - **Missing date warning chip** (amber) if test/insurance date not set, or no service records
  - Animated expand/collapse for settings section (schedule info + custom window field)
  - Schedule info: "60, 30 ימים לפני · שבועי · פעמיים ביום בשבוע האחרון"
  - Custom "start reminding" days field (default 60, clamped 1–365)
  - Form state lives in ViewModel — survives recomposition + DB flow re-emissions
- `CarRemindersViewModel`:
  - Injects `MaintenanceRecordRepository` to compute `SERVICE_DATE` next-due date
  - `formState: StateFlow<Map<ReminderType, ReminderUiState>>` — initialized once from DB
  - `lastMaintenanceDate: StateFlow<Long?>` — for SERVICE_DATE countdown in UI
  - `updateEnabled()` / `updateDays()` mutate form without saving to DB
  - `saveReminders(onSaved)` — writes to DB and calls callback
  - `buildDefaultReminders()` — always creates all 3 types (TEST on, SERVICE on, INSURANCE on if date exists)
- **Escalation schedule in `ReminderCheckWorker`**:
  - Fires at: 60d, 30d, weekly (23, 16, 9), then every run ≤7d (≈twice/day)
  - `SERVICE_DATE` now works: `lastMaintenanceDate + 365d` as expiry
  - Stable notification IDs: `carId * 10 + type.ordinal` (survives reminder re-inserts)
  - Injects `MaintenanceRecordRepository`
- **WorkManager: 12-hour interval** (was 1 day) + `ExistingPeriodicWorkPolicy.UPDATE`
- **Navigation**: `CarProfileScreen` now has a "Reminders" button (Documents + Reminders share a row, 50/50)
- Post-car-creation reminder prompt: ModalBottomSheet with icon + description

### Phase 6 — Settings
- `AppPreferencesDataStore`: DataStore wrapper for `theme_mode`, `language` (iw/en), `notification_permission_asked`
- `SettingsViewModel`: reads/writes `ThemeMode` + `language`
- **Theme persists** across sessions via DataStore
- `SettingsScreen` (full redesign):
  - Each section in a `Card` with section icon badge + title
  - **Theme section**: FilterChip row — System / Light / Dark (with check icon when selected)
  - **Language section**: FilterChip row — עברית / English. Tapping calls `AppCompatDelegate.setApplicationLocales()` → activity recreates immediately
  - **Notifications section**: shows status; button to enable if disabled
  - **Export section**: PDF / Excel / JSON rows with icons + "Coming Soon" badge (UI only)
  - **About section**: version + developer name, with icons
- `MainActivity` extends `AppCompatActivity` (required for `AppCompatDelegate.setApplicationLocales()`)

### Phase 9 — UI/UX Polish Pass

- **Insets fix**: `MainScreen` Scaffold now has `contentWindowInsets = WindowInsets(0)`. Removed `consumeWindowInsets` from NavHost. Each inner screen handles its own insets cleanly via its Scaffold + TopAppBar `windowInsets`. Eliminates the double status-bar-height spacing on the Cars screen.
- **CarCard redesign**: Full-width card (~220dp). Photo fills top 148dp with `ContentScale.Crop`. Gradient scrim (transparent → black 72%) overlaid on bottom of photo. Make/model as `headlineSmall Bold White` text overlaid on the gradient. Left accent bar spans full card height via `IntrinsicSize.Min`. Info row below photo: year · separator dot · license plate badge · km. Status chips row with color dot pushed to end. `ElevatedCard` with 4dp elevation. Placeholder (no photo) uses a `primaryContainer → secondaryContainer` linear gradient with a large car icon. `ColorDot` now delegates to `carColorToComposeColor()` from `CarColorUtils.kt` (linter extracted the mapping).
- **AddCarSheet color picker**: Replaced free-text color field with `FlowRow` of 15 color circles (44dp each). Ring-style selection: primary-colored outer ring + smaller inner swatch. White check icon on a semi-transparent scrim for the selected state. Text field below always visible — shows current value, accepts free-text. Photo picker area upgraded to 180dp, gradient background placeholder, camera icon in a circle, "tap to change" pill overlay when photo is set.
- **AddMaintenanceRecordSheet**: Dynamic description placeholder per record type (MAINTENANCE / REPAIR / WEAR). `FlowRow` of `SuggestionChip` quick-fill chips above the description field — chip colors match the selected record type accent (primary/error/tertiary containers). `SheetSectionHeader` upgraded: `fontWeight = Bold` + `HorizontalDivider` (0.5dp outlineVariant) + `Spacer` before the header for breathing room. Vertical spacing increased throughout.
- **New utility**: `CarColorUtils.kt` — `carColorToComposeColor(colorName: String): Color?` mapping Hebrew/English color names → Compose Colors. Used by CarCard.

### Phase 8 — Scope Cleanup + Receipt/Certificate Photos

- **Removed comprehensive insurance** from entire app: `Car` entity, `ReminderType` enum, `DocumentType` enum, `AddCarSheet`, `AddCarViewModel`, `StatusBanner`, `CarRemindersScreen`, `CarRemindersViewModel`, `ReminderCheckWorker`. Only טסט + ביטוח חובה remain.
- **DB bumped to v6** (removed `comprehensiveInsuranceExpiryDate` column from `cars` table)
- **Receipt photos on maintenance records**: `AddMaintenanceRecordSheet` shows photo picker (PickVisualMedia + takePersistableUriPermission), thumbnail with clear button if set. `RecordDetailSheet` shows full-width 180dp thumbnail when `receiptUri` is present.
- **Test certificate photos**: `AddTestRecordSheet` has same photo picker pattern for `certificateUri`. `TestRecordDetailSheet` shows thumbnail when set.
- **Test reminder notification** now appends "אל תשכח להביא תעודת ביטוח" to the notification message.
- **Documents screen visual upgrade**: type-specific icon badges (Security/DirectionsCar), full-width 180dp thumbnail instead of 80dp square, replace/delete button row, `contentWindowInsets = WindowInsets(0)`.

### Phase 7 — Test History
- `TestRecord` entity: id, carId (FK → Car CASCADE), date: Long, passed: Boolean, notes: String?, createdAt: Long
- `TestRecordDao`, `TestRecordRepository`, `TestRecordRepositoryImpl`
- `TestHistoryViewModel`: `_carId` flow → records StateFlow + insert/update/delete
- `TestHistoryScreen`: full navigation destination (not a sheet)
  - LazyColumn: cards with left accent bar (green = passed, red = failed), date, pass/fail badge, optional notes
  - FAB → `AddTestRecordSheet`: date picker, pass/fail Switch (green/red colors), optional notes
  - Tap card → detail sheet → edit / delete
- `Screen.TestHistory` added to navigation graph
- `CarProfileScreen` "Test History" button now navigates to `TestHistoryScreen` (removed "coming soon" sheet)
- **DB version 4** with `fallbackToDestructiveMigration()` (dev only)

### Architecture Decisions Made
- **No center FAB**: Each screen owns its FAB. Cleaner M3 design.
- **No mapper layers**: Room entities used directly as domain models.
- **Reminders always tied to carId**: No standalone reminders.
- **No km tracking outside records**: Km only entered when logging a maintenance record.
- **Test History = full screen**: Not a bottom sheet — it has a FAB and LazyColumn.
- **RTL is conditional**: Only forced when locale is Hebrew (he/iw). English uses system LTR.
- **Nested Scaffold insets fixed**: `consumeWindowInsets(paddingValues)` on NavHost in `MainScreen.kt`.
- **All bottom sheets**: `skipPartiallyExpanded = true` — content always starts at top.
- **AppCompatActivity**: Required for `AppCompatDelegate.setApplicationLocales()`.

### Car Make/Model Data
- `CarMakeData.kt` in `util/`: 35+ makes, per-make model lists (150+ models total)
- `ExposedDropdownMenuBox` for make with live filtering, model suggestions update when make changes
- Free text always allowed as fallback for both make and model

---

## Current State

**What works (compile-verified):**
- Full car CRUD (add with dropdowns, view, edit, delete)
- Car profile with full-bleed hero, status banner (טסט + ביטוח + שירות תקופתי), 3-tile action row
- Maintenance history with full CRUD, left accent bar, type icons, receipt photo attach + thumbnail in detail
- Documents screen: one card per type (TEST/INSURANCE), editable expiry + optional file, `VehicleRecordHistoryScreen`
- Reminders dashboard (`RemindersScreen` → per-car `CarRemindersScreen`): urgency colors, countdown chips, animated expand/collapse, escalation schedule
- Settings: theme switching (persists), language switching (system/iw/en), notification permission, export section (UI placeholder), version info
- Splash screen
- Navigation: Cars (HorizontalPager) → Car Profile → Maintenance History / Documents / Car Reminders / VehicleRecordHistory
- Notification permission asked once on first launch (bottom sheet)
- Export: PDF + JSON functional (`ExportViewModel`, `PdfExporter`, `JsonExporter`); Excel coming soon

**Intentionally removed / no UI:**
- `TestHistoryScreen` — deleted. `TestRecord` entity + DB table kept for export only. No UI to add new test records.
- `CarDocument` entity — removed entirely. Replaced by `VehicleRecord`. All related files (`CarDocumentDao`, `CarDocumentRepositoryImpl`, `CarDocumentRepository`, `CarDocument`) deleted.

**WorkManager:** 12-hour periodic check (`ReminderCheckWorker`) with escalation schedule: 60d, 30d, weekly (9/16/23), then every run in last 7 days (≈twice daily). SERVICE_DATE uses last MAINTENANCE record + 365d.

### Phase 9 — Premium UI/UX Polish

**Car Profile Screen (full overhaul):**
- **Full-bleed hero** (248dp): Column skips top padding — hero sits behind the transparent TopAppBar/status bar for a cinematic, immersive look
- **Top scrim gradient** on hero ensures edit/delete icons are always readable on any photo
- **Edit/Delete icon pill**: wrapped in a semi-transparent dark `Surface` for contrast on any background
- **Photo hero**: stronger bottom gradient (0.88 alpha), `headlineSmall` make/model, frosted-glass license plate badge, color accent dot next to car name
- **Placeholder hero**: gradient tinted by car's actual color via `CarColorUtils`; falls back to `primaryContainer` for unknowns
- **Section labels**: `labelMedium` primary-colored headers ("סטטוס", "פרטי הרכב", "פעולות") above each card section
- **Status banner**: two individual mini-cards (one per status type — test + insurance). Always shows both even when date is null. Each colored by own status level. Expired items get a 2dp `StatusRed` border. Right-side day-count badge: big number + "ימים", "פג!" for expired, "היום" for today, `?` icon for not-set.
- **Action buttons → icon-forward Card tiles**: 2×2 grid, 100dp height, icon on top + label below. Maintenance = `primaryContainer`, others = `surfaceContainerLow`.
- **Stats CTA**: `totalRecords < 2` → tappable nudge card "הוסף את הטיפול הראשון" (navigates to maintenance history). Stats card only shown when ≥ 2 records (per spec).
- **Stats card icons**: Build / CalendarToday / AttachMoney / Analytics icons on each stat item
- **Notes section**: wrapped in Card, consistent with rest of screen
- **More breathing room**: 20dp spacers between sections (was 12–16dp)
- **`util/CarColorUtils.kt`** (new file): `carColorToComposeColor(name): Color?` — shared utility
- **`CarCard.kt`**: removed duplicated color map, now uses `carColorToComposeColor`

**Settings Screen:**
- **3rd language chip "מערכת"**: calls `setApplicationLocales(getEmptyLocaleList())` to reset to device locale. Stored as `"system"`. Default for new installs.
- **Theme chips**: always show mode icon (BrightnessAuto / LightMode / DarkMode) in both selected and unselected states
- **Language chips**: PhoneAndroid icon for System, Language icon for Hebrew/English
- **Export rows**: "Coming Soon" text badge → `Icons.Outlined.Lock` icon (cleaner, no redundant text)
- **Developer row**: tappable with ripple (no nav action)
- **DataStore + ViewModel**: language default changed from `"iw"` to `"system"`

---

### Phase 10 — Documents Overhaul + Car Profile Restructure

- **VehicleRecord** entity (new table `vehicle_records`): `type` (TEST/INSURANCE), `expiryDate`, `fileUri?`, `isActive`, `createdAt`
- **CarDocument removed** from DB — replaced by VehicleRecord
- **AppDatabase** bumped v6 → v7
- **CarDocumentsScreen** fully rewritten: one card per type (TEST/INSURANCE), editable expiry + optional file attach, "היסטוריית מסמכים" button
- **VehicleRecordHistoryScreen**: full list of all records with active/archived badges
- **CarProfileScreen**: Test History button removed, 3-tile action row (Maintenance, Documents, Reminders)
- **StatusBanner**: now shows 3 mini-cards — Test, Insurance, Periodic Service (last MAINTENANCE + 365d)
- **CarProfileViewModel**: `nextServiceDueMs` StateFlow derived from last MAINTENANCE record
- **ReminderType**: `INSURANCE_COMPULSORY_EXPIRY` → `INSURANCE_EXPIRY`
- **Navigation**: `TestHistory` route replaced by `VehicleRecordHistory`
- **strings.xml**: "ביטוח חובה" → "ביטוח" across all labels; all new document strings added

### Phase 11 — CarsScreen Overhaul + UI Polish

- **CarsScreen**: `LazyColumn` replaced by `HorizontalPager` card catalog. `CarPager.kt` in `presentation/components/`. Cards are large, full-bleed with photo hero + gradient scrim.
- **RemindersScreen**: fully rewritten as a dashboard — `RemindersDashboardViewModel` drives a per-car summary view. Each car row shows urgency chip across all reminder types.
- **MaintenanceRecordCard**: upgraded to `ElevatedCard`, receipt thumbnail shown inline, `IntrinsicSize.Min` for left accent bar alignment.
- **OneMoreServiceCtaCard**: needs design upgrade (currently too basic — progress indicator + stats teaser planned).
- **CarRemindersScreen**: functional, design polish pass pending.

## What's Next (Priority Order)

1. **OneMoreServiceCtaCard** design upgrade — progress teaser with LinearProgressIndicator
2. **CarRemindersScreen** design polish pass
3. **Excel export** — PDF + JSON done, Excel remaining
4. **Proper DB migrations** — replace `fallbackToDestructiveMigration()` before release (DB at version 7)
5. **Statistics deep-dive** — cost per km chart, most common repair type, spending trend

---

## Known Improvement Areas

- `AddMaintenanceRecordSheet` uses local state — fine for now.
- `CarsScreen` instantiates three ViewModels — `CarRemindersViewModel` only used for `enableDefaultReminders()`.
- `STATUS_YELLOW` threshold: 8–30 days. Could be user-configurable per reminder type (currently only the outer window is configurable).

---

## Tech Stack (Current)

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
| AppCompat | 1.7.0 | Language switching (`AppCompatDelegate`) |
| Coroutines | 1.9.0 | Async |
| KSP | 2.0.21-1.0.28 | Annotation processing |
| compileSdk | 35 | Stable |
| minSdk | 26 | Android 8.0+ |
