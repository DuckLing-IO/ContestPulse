<div align="center">

# Contest Pulse

**A local-first Android contest calendar and reminder app for competitive programmers**

Aggregate Codeforces, AtCoder, Luogu, Nowcoder, and custom public schedules with local times, countdowns, favorites, and reminders.

<p>
  <img alt="Version" src="https://img.shields.io/badge/version-1.0-000000?style=flat-square">
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-000000?style=flat-square&logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-1.9.24-000000?style=flat-square&logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/UI-Jetpack_Compose-000000?style=flat-square&logo=jetpackcompose&logoColor=white">
  <img alt="Local First" src="https://img.shields.io/badge/privacy-local_first-000000?style=flat-square">
</p>

[简体中文](README.md) · **English**

[Interface preview](#interface-preview) · [Highlights](#highlights) · [Data sources](#supported-data-sources) · [Getting started](#getting-started) · [Custom sources](#adding-a-custom-source) · [Architecture](#architecture) · [Verification](#project-verification)

</div>

---

## Overview

Contest Pulse is an Android app with no application backend and no account system. It reads upcoming programming contests from public APIs or pages, normalizes their fields and times, stores them locally, and provides favorites, countdowns, calendar integration, and local reminders.

Registration, sign-in, and submissions always happen on the official platform. The app does not store platform credentials, cookies, or tokens, and it never uploads favorites or reminders.

Download the officially signed 1.0 APK from [GitHub Releases](https://github.com/DuckLing-IO/ContestPulse/releases/tag/v1.0).

## Interface preview

<table>
  <tr>
    <th width="33%">Contests</th>
    <th width="33%">Saved</th>
    <th width="33%">Settings</th>
  </tr>
  <tr>
    <td><img src="img4md/比赛.jpg" alt="Contests: schedules, countdowns, and filters" width="100%"></td>
    <td><img src="img4md/我的.jpg" alt="Saved: local favorites and status filters" width="100%"></td>
    <td><img src="img4md/设置.jpg" alt="Settings: data sources and background sync" width="100%"></td>
  </tr>
  <tr>
    <td align="center">Aggregated schedules, countdowns, and collapsible filters</td>
    <td align="center">Offline favorites organized by contest status</td>
    <td align="center">Built-in/custom sources and synchronization preferences</td>
  </tr>
</table>

## Highlights

| Capability | Description |
| --- | --- |
| Multi-platform aggregation | Built-in Codeforces, AtCoder, Luogu, and Nowcoder sources, plus user-defined public schedules |
| Local-first storage | Room cache remains available offline and is not cleared when a refresh fails |
| Per-source isolation | One failed source does not cancel the others; the home screen identifies failures precisely |
| Schedule browsing | Next-contest hero, running/today/tomorrow/this-week/later groups, and live countdowns |
| Collapsible filters | Filter by platform, next 7/30 days, Rated status, or favorites |
| Favorites and reminders | Preset and custom offsets with exact/inexact AlarmManager fallback |
| System integration | Official-page Custom Tabs, calendar insert intents, and notification deep links |
| Background sync | DataStore preferences and WorkManager with Wi-Fi and 6/12/24-hour options |
| Monochrome design system | Jetpack Compose, light/dark themes, floating rounded navigation, no-ripple press feedback, and spring motion |

## Supported data sources

| Source | Integration | Time handling |
| --- | --- | --- |
| [Codeforces](https://codeforces.com/apiHelp/methods) | Official `contest.list` JSON API | epoch seconds → UTC `Instant` |
| [AtCoder](https://atcoder.jp/contests/) | Public Upcoming / Daily HTML | page offset → UTC `Instant` |
| [Luogu](https://www.luogu.com.cn/contest/list) | Public contest-list HTML | `Asia/Shanghai` → UTC `Instant` |
| [Nowcoder](https://ac.nowcoder.com/acm/contest/vip-index) | Public Nowcoder series and university-contest HTML | epoch milliseconds → UTC `Instant` |
| Custom sources | JSON, iCalendar, JSON-LD, embedded JSON, semantic HTML, or CSS field mapping | user-confirmed zone → UTC |

AtCoder, Luogu, Nowcoder, and custom HTML pages are not stable APIs. Their parsers use fail-closed validation and minimal fixture regression tests. A structural failure affects only that source and preserves its previous cache.

## Getting started

### Requirements

| Item | Version |
| --- | --- |
| Android | 8.0 / API 26 or newer |
| Compile / Target SDK | API 34 |
| JDK | 17 |
| Gradle Wrapper | 8.9 |
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 1.9.24 |

### Android Studio

1. Clone the repository and open its root directory.
2. Select JDK 17 in Android Studio and wait for Gradle Sync.
3. Start an Android 8.0 (API 26) or newer emulator, or connect a device.
4. Run the `app` configuration.
5. Wait for the first sync, or tap/pull to refresh the contest screen.

On the first import, create a machine-specific `local.properties` file in the repository root. It is ignored by Git.

```properties
sdk.dir=<absolute path to your Android SDK>
```

### Command-line build

Windows PowerShell:

```powershell
./gradlew.bat assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

macOS / Linux:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. APKs, signing material, and local SDK configuration are excluded by `.gitignore`.

Official release builds read the untracked `keystore.properties` file from the repository root. Copy [`keystore.properties.example`](keystore.properties.example), fill in a dedicated signing key, and run:

```powershell
./gradlew.bat assembleRelease
```

If you only want to install the stable app, use the signed and verified APK from [Releases](https://github.com/DuckLing-IO/ContestPulse/releases); no local signing setup is required.

## Adding a custom source

Open **Settings → Data sources → Custom sources**:

1. Enter a source name, a public HTTPS contest-list URL, and its default time zone.
2. Keep “Auto detect” selected and choose “Parse and preview.”
3. Verify contest titles and local times, then save and enable the source.
4. If automatic detection fails, open HTML field mapping and provide CSS selectors for the contest item, title, and start time. End time, link, and date-time pattern are optional.

A successful preview is mandatory before saving. Every custom source has independent enablement, synchronization, and error state. Deleting one also removes its local contests, favorites, and reminders.

### Custom-source security boundary

- Only public HTTPS URLs are accepted; credentials in URLs are rejected.
- No page JavaScript, cookies, authenticated sessions, or Authorization headers.
- Localhost, private, link-local, CGNAT, and IPv6 ULA addresses are blocked.
- Every redirect is revalidated and the connection is pinned to validated DNS addresses.
- Maximum 3 redirects, 1 MB decompressed body, and 200 upcoming/running contests.
- Pages requiring sign-in, CAPTCHA, infinite scrolling, or anti-automation challenges are unsupported.

When a URL belongs to a built-in platform, the preview recommends disabling that built-in source to avoid duplicate contests.

## Architecture

```text
Compose UI / ViewModel
          ↓
Domain models · filters · repository contracts
          ↑
Offline-first repository
    ├── Room: contests · favorites · reminders · sync status
    ├── Retrofit / OkHttp / Jsoup / Kotlinx Serialization
    ├── DataStore: sync preferences · custom source definitions
    ├── AlarmManager / Notification / Calendar Intent
    └── WorkManager: periodic source sync
```

```text
app/src/main/java/io/duckling/contestpulse/
├── core/          # Design system, Room, calendar, and safe external links
├── data/          # Built-in/custom parsing, DataStore, and offline repository
├── domain/        # Models, filtering/time logic, and repository contracts
├── feature/       # Contests, details, favorites, settings, and custom sources
├── navigation/    # Three-tab navigation, child routes, and notification deep links
├── reminder/      # Alarms, notifications, permissions, and rescheduling
└── sync/          # WorkManager scheduling and background sync entry points
```

Additional technical documentation (currently in Chinese):

- [Architecture](docs/ARCHITECTURE.md)
- [Data-source maintenance](docs/DATA_SOURCES.md)
- [Roadmap](docs/ROADMAP.md)
- [Release checklist](docs/RELEASE_CHECKLIST.md)

## Project verification

Contest Pulse 1.0 has passed the following checks:

```powershell
./gradlew.bat testDebugUnitTest
./gradlew.bat lintDebug
./gradlew.bat assembleDebug
./gradlew.bat assembleDebugAndroidTest
./gradlew.bat assembleRelease
```

- 51 JVM unit tests passed.
- Android Lint: 0 errors; current warnings are dependency/tooling update suggestions only.
- Debug and AndroidTest APK builds passed.
- Release R8 minification and resource shrinking passed.
- Read-only live validation passed for both Nowcoder public categories; offline tests cover its double-encoded HTML entities.

Compose UI and Room instrumentation tests require an attached emulator or device:

```powershell
./gradlew.bat connectedDebugAndroidTest
```

## Permissions and privacy

| Permission | Purpose |
| --- | --- |
| `INTERNET` | Read public contest data from sources enabled by the user |
| `POST_NOTIFICATIONS` | Show user-created reminders on Android 13+ |
| `SCHEDULE_EXACT_ALARM` | Improve reminder precision when allowed, with automatic fallback |
| `RECEIVE_BOOT_COMPLETED` | Restore valid local reminders after reboot |

Android cloud backup is disabled. The app does not log cookies, authentication headers, accounts, or passwords. Favorites, settings, custom sources, and reminders stay on the current device. Contest Pulse is not officially affiliated with any listed contest platform.

## Known limitations

- WorkManager and inexact alarms are subject to Android and vendor power-management policies and cannot guarantee exact timing.
- HTML page changes can temporarily break a source; other sources and existing cache remain available.
- The current target is API 34. A store release must update it according to then-current policy and repeat device regression testing.
- Runtime screenshots are not included yet; release screenshots should be captured from real devices in light, dark, and large-text modes.
- This repository does not currently declare an open-source license. Public visibility does not automatically grant rights to copy, modify, or redistribute the code.
