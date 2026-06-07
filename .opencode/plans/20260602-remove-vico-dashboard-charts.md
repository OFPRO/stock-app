---
status: in-progress
branch: feat/omni-framework
timestamp: 2026-06-02T03:44:29+01:00
files_modified:
  - android/feature/dashboard/src/main/java/com/app2/feature/dashboard/DashboardScreen.kt
  - android/feature/dashboard/build.gradle.kts
  - android/gradle/libs.versions.toml
---

## Working on: Remove Vico, fix Dashboard charts with Canvas

### Summary

Vico 2.0.0-beta.2 library was unresolvable at build time (`lineSeries`, `columnSeries`, `CartesianChartHost` all unresolved). Replaced both Vico charts (SalesChart line chart, InvoicesStatusChart column chart) with pure Compose Canvas implementations. Removed Vico from all build files and version catalog. Build, lint, test all pass. APK reinstalled on emulator.

### Decisions Made

- **Canvas over Vico for all charts** — Vico beta had import resolution issues in this project's Gradle/mirror config. Canvas is zero-dependency and compiles reliably. The loss of interactive features (touch tooltips, zoom) is acceptable for a dashboard read-only view.
- **Keep Canvas for existing custom charts** — CombinedTrendChart, HorizontalBarChart, DonutChart were already Canvas-based; SalesChart and InvoicesStatusChart are now also Canvas-based. Unified approach lowers maintenance surface.
- **Removed Vico entirely from version catalog** — prevents accidental re-import by other modules. Three library entries (`vico-core`, `vico-compose`, `vico-compose-m3`) plus version key purged.

### Remaining Work

1. Start Flask backend (`python app.py`) to serve API data for Android emulator
2. Manual test on emulator — verify Dashboard renders data, POS flow works
3. Update OMNI_STATE.yaml to reflect this round's completion
4. Tag and push if user requests

### Notes

- `MaterialTheme.colorScheme.surface` inside Canvas draw scope caused "Composable invocations can only happen from the context of a @Composable function". Fix: capture `val surfaceColor = MaterialTheme.colorScheme.surface` before the Canvas lambda.
- `Stroke` import was mistakenly removed during Vico import cleanup but is still required by Canvas `drawPath` and `drawArc` calls.
- `LaunchedEffect` and `RoundedCornerShape` were unused after Vico removal.
- DonutChart already captures colors list before Canvas, so only `surfaceColor` needed extraction.
