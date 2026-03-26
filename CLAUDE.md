# Allergia — Development Notes

## Project Overview

Android allergy-tracking diary app built with Kotlin / Jetpack Compose / MVVM.

## Tech Stack

- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM + Hilt DI
- **Database**: Room 2.6.1 (version 2)
- **Networking**: Retrofit + OkHttp → OpenRouter API
- **Preferences**: DataStore (single instance in `utils/AppDataStore.kt`)
- **Image handling**: ExifInterface + BitmapFactory

## Key Architecture Rules

### DataStore — Single Instance
There is exactly **one** DataStore instance in the app:
```
utils/AppDataStore.kt  →  val Context.appDataStore
                           object PreferenceKeys
```
All classes must import from here. **Never** add another `by preferencesDataStore(...)` delegate — Android DataStore crashes if two instances point to the same file.

### AI Models (centralized in `api/ModelConstants.kt`)
```
Models.FOOD_PHOTO  = google/gemma-3-4b-it:free      (vision, food photos)
Models.ANALYSIS    = google/gemini-3.1-flash-lite-preview  (allergy analysis)
Models.LABEL_SCAN  = google/gemini-3.1-flash-lite-preview  (ingredient label scan)
```

### API Key
Stored at runtime in DataStore under `PreferenceKeys.OPENROUTER_API_KEY`.
Never hardcoded. User enters it in Settings → SettingsScreen.
CI injects it via `secrets.AI_API_KEY` → `gradle.properties`.

## Building

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## CI / GitHub Actions

Workflow: `.github/workflows/build.yml`
Required secret: `AI_API_KEY` (OpenRouter API key, injected into `gradle.properties`)

## Database

Room DB version **2**. Uses `fallbackToDestructiveMigration()` (dev build).
Entities: `FoodItem`, `Medication`, `SkinCondition`, `AllergySymptom`, `AnalysisResult`, `HouseholdProduct`.

## Photo Pipeline

1. Camera/gallery URI → `ImageUtils.uriToBase64()` (max 800px, adaptive JPEG quality, EXIF correction)
2. Base64 data URL → OpenRouter Vision API
3. Response JSON → parsed into result objects
4. `FoodPhotoService` — food recognition (Gemma 3 4B free)
5. `LabelPhotoService` — INCI ingredient label analysis + local allergen dictionary cross-check

## Module Map

```
api/          — OpenRouter API client, DTOs, service classes
data/
  database/   — Room DB, DAO, TypeConverters
  models/     — Entity + data classes
  repository/ — DiaryRepository
di/           — Hilt AppModule
ui/
  components/ — Reusable Compose components
  screens/    — Screen composables
  theme/      — Material3 theme
  viewmodels/ — MVVM ViewModels
utils/        — AppDataStore, ImageUtils
```
