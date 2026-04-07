# AutoCare - Project Rules & Working Agreement

> **For any Claude Code session working on this project:** Read this file FIRST, then read `PROJECT_SPEC.md`. These are the source of truth. But read them as a thinking collaborator, not a robot - see "Your Freedom" section below.

---

## 🎯 Project Identity

- **Name:** AutoCare
- **Type:** Android native app (Kotlin + Jetpack Compose)
- **Owner:** Hananel Sabag (software engineer)
- **Purpose:** Personal car maintenance tracker. Portfolio-quality. Open-source on GitHub.
- **Primary language:** Hebrew (RTL). English to be added later via `strings.xml`.

---

## 🤝 Your Freedom & How We Work Together

**You are a collaborator, not an executor.** I want your brain, not just your hands.

- If something in the spec doesn't feel right to you - **say so before building it**. Maybe you see a better pattern, a cleaner architecture, a more modern library. I want to hear it.
- If mid-implementation you realize there's a better approach - **stop and tell me**. We'll discuss and decide together.
- Over-planning leads to ugly, rigid code. Things evolve as we build. That's fine and expected.
- **Before starting any new phase or major task:** Read what I've asked for, think about it, and tell me what you think. Suggest improvements. Flag concerns. Only start coding once we're aligned.
- If you have a **good idea** that wasn't in the spec - propose it. I'd rather hear "what about X?" than have you silently build something suboptimal.

**The rule:** Think first, propose, get alignment, then build. Don't just dive in.

---

## 🚫 Decisions Already Made - Do Not Re-Ask

These were discussed at length during planning. **Do not bring them up as suggestions or questions** - they're settled. You can still flag a concern if you genuinely think one is wrong, but don't waste turns "suggesting" things from this list.

### Things we explicitly REJECTED:
- ❌ **No kilometer tracking outside of maintenance records.** Users will NOT be asked to update km regularly. Km is only entered when adding a maintenance record (because that's when you naturally see it). Don't suggest "daily km logging", "trip tracking", "odometer reminders", etc.
- ❌ **No fuel tracking in v1.** It's deferred. Don't propose adding it. It forces user input on every refuel which contradicts the project philosophy.
- ❌ **No horizontal scrolling anywhere.** It's a mobile anti-pattern. Use vertical scroll, tabs, or pagination instead. Don't suggest swipeable horizontal lists for primary content.
- ❌ **No popups/AlertDialogs for input.** Use `ModalBottomSheet` for all user input flows. The ONLY exception is destructive confirmations (delete car, delete record) - those can use AlertDialog.
- ❌ **No forced kilometer field on repairs/wear items.** Km is **required only for maintenance records** (oil change type). For repairs and wear items, km is optional.
- ❌ **No rigid notification schedules.** Users get full freedom to add/edit/delete reminders, set specific times, set custom days-before. Don't hardcode "always notify 30 days before". Defaults are defaults, not laws.
- ❌ **No OCR for receipts.** Overkill for v1.
- ❌ **No VIN scanner / no auto car data lookup APIs.** Manual entry is fine.
- ❌ **No 4+ item bottom nav.** We use 3 items + center FAB. Don't suggest adding a 4th tab.

### Things we explicitly DECIDED:
- ✅ **3 record types:** `MAINTENANCE` (default = 10K service, requires km), `REPAIR` (one-off fixes, km optional), `WEAR` (tires/brakes/battery/wipers, km optional). Database schema must support adding more types in the future without migration pain.
- ✅ **Smart status banner** at top of car profile - green/yellow/red for test (טסט) and insurance urgency. Always visible, impossible to miss.
- ✅ **Documents** (insurance compulsory, insurance comprehensive, vehicle license/test) live inside the car profile. Each is image OR PDF, with expiry date.
- ✅ **Statistics** only appear when there are at least 2 maintenance records to compare.
- ✅ **3 export formats:** PDF (beautiful, with photos and full data), Excel, JSON (for backup/restore).
- ✅ **Material 3 with Material You (dynamic color), Light + Dark + System** - all three from day one, designed properly so contrast works in both themes.
- ✅ **i18n from day one.** Hebrew first, mechanism for language switching ready immediately, English added later by translating one file.
- ✅ **Bottom nav: 3 items + center FAB.** FAB opens a bottom sheet with quick-add actions.
- ✅ **MVVM + Clean Architecture** (data / domain / presentation).
- ✅ **Tech stack:** Kotlin, Compose, Material 3, Room, DataStore, Hilt, Navigation Compose, Coroutines+Flow, WorkManager, Coil. Use latest stable versions of everything. No deprecated APIs.

### My philosophy (internalize this):
> **"User freedom with smart defaults."** Every configurable thing has a sensible default but is fully customizable. Don't be lazy and hardcode things "for simplicity" - giving the user control is usually 3 extra lines of code and a massive UX win. We're engineers. We give power users power. Defaults handle the casual user.

---

## 🚨 Absolute Rules (Never Break)

### 1. NO Hardcoded User-Facing Strings - EVER
Every user-facing string must come from `strings.xml`. No exceptions for "just this once".

❌ Wrong:
```kotlin
Text("הוסף רכב")
Toast.makeText(context, "נשמר בהצלחה", Toast.LENGTH_SHORT).show()
```

✅ Correct:
```kotlin
Text(stringResource(R.string.add_car))
Toast.makeText(context, context.getString(R.string.saved_successfully), Toast.LENGTH_SHORT).show()
```

This includes: button labels, screen titles, error messages, toasts, snackbars, content descriptions, empty states, validation errors, notification text, dialog text - **everything the user can see**.

The only allowed hardcoded strings: log messages (developer-only), debug code, database/technical identifiers, code comments.

**When adding strings:**
- Add to `res/values/strings.xml` with a clear `snake_case` key
- Group related strings together with XML comments (`<!-- Car Profile Screen -->`)
- Use descriptive keys: `error_invalid_kilometers` not `error_1`
- If unsure whether a string already exists - search `strings.xml` first, don't duplicate

### 2. RTL-First Layouts
The app is in Hebrew. Every layout must work perfectly in RTL.
- Use `start`/`end`, never `left`/`right`
- Test mentally: would this look right mirrored?
- Icons that have direction (arrows, etc.) must respect RTL via `autoMirrored` or layout direction

### 3. Theme Tokens, Never Hardcoded Colors
```kotlin
// ❌ Wrong
color = Color(0xFF1E88E5)
// ✅ Correct  
color = MaterialTheme.colorScheme.primary
```
Always use `MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*`. This is how light/dark themes stay consistent.

### 4. No Deprecated APIs
If Android Studio shows a strikethrough or deprecation warning - use the modern replacement. We're building this fresh, no reason to ship deprecated code.

### 5. Bottom Sheets, Not Dialogs (for input)
Use `ModalBottomSheet` for all user input flows. AlertDialog is reserved for destructive confirmations only.

### 6. No Horizontal Scrolling
Vertical scroll, tabs, or pagination instead. Always.

---

## 📁 Folder Structure (Proposed - You Can Refine)
```
com.hananelsabag.autocare/
├── data/
│   ├── local/
│   │   ├── database/        # Room DB, DAOs
│   │   ├── entities/        # Room entities
│   │   └── preferences/     # DataStore
│   ├── repository/          # Repository implementations
│   └── model/               # DTOs
├── domain/
│   ├── model/               # Domain models
│   ├── repository/          # Repository interfaces
│   └── usecase/             # Use cases
├── presentation/
│   ├── theme/               # Material 3 theme
│   ├── components/          # Reusable composables
│   ├── navigation/          # Nav graph
│   └── screens/
│       ├── onboarding/
│       ├── cars/
│       ├── carprofile/
│       ├── maintenance/
│       ├── documents/
│       ├── reminders/
│       ├── statistics/
│       └── settings/
├── notifications/           # WorkManager workers
├── export/                  # PDF, Excel, JSON export
├── di/                      # Hilt modules
├── util/                    # Extensions, helpers
└── MainActivity.kt
```

If you have a better structure - propose it before scaffolding.

---

## 🔄 Cross-Session Consistency

Multiple Claude Code sessions will work on this project over time. To prevent drift:

1. **Always read `CLAUDE.md` and `PROJECT_SPEC.md` at the start of every session.** Even if the user doesn't explicitly ask. They are your context.
2. **Before adding any new string, search `strings.xml` for similar existing keys.** Don't create duplicates like `save_button` and `btn_save`.
3. **Before adding a new dependency, check `build.gradle` for what's already there.** Don't add a second JSON library if Kotlinx Serialization is already in.
4. **Before creating a new component, search `presentation/components/` for an existing one you can reuse or extend.**
5. **Match the existing code style.** Look at recent files before writing new ones - naming, structure, comment style, file organization.
6. **If you change something architectural** (folder structure, naming convention, library choice) - **update this file** so future sessions know.

---

## 🧪 Testing & Verification

- After implementing a feature, **explain what you built and how to test it** - don't just say "done".
- For bug fixes and reviews, prefer **reading and analyzing over rewriting**. The user prefers a calm, careful review pass over an aggressive refactor when the task is "check this".
- Don't refactor unrelated code "while you're there". Stay focused.

---

## 💬 Communication Style

- The user is a software engineer, comfortable in English and Hebrew. Code/technical discussion in English is fine. He may chat in Hebrew.
- Be direct. Skip excessive pleasantries.
- When proposing options, give your **recommendation** with reasoning, not just a neutral list.
- When you're unsure, **ask** - don't guess and build something that has to be torn out.
- When you disagree with a request, **say so respectfully and explain why**. The user wants pushback when it's warranted.

---

## 🚀 Workflow

1. **Read** `CLAUDE.md` + `PROJECT_SPEC.md` at session start.
2. **Understand** the current task.
3. **Think** - is there anything in the spec that seems off for this task? Anything that could be done better?
4. **Propose** - tell the user what you're about to do, flag any concerns, suggest improvements.
5. **Align** - wait for confirmation.
6. **Build** - implement cleanly, following all rules above.
7. **Report** - explain what you built, how to test, any decisions made along the way.
8. **Commit suggestion** - suggest a clear commit message.

---

## ❤️ Final Note

This project is built to be a portfolio piece **and** a real tool the owner uses on his own cars. Quality matters. Polish matters. But over-engineering and rigidity are also enemies. Stay flexible, stay thoughtful, push back when it matters, and build something we're both proud of.

Let's make it great. 🔧🚗