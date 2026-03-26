# Allergia 🌿

Android app for tracking food allergies and identifying triggers.

## Features

- **Daily diary** — food/drinks, medications, skin condition (6 parameters), 16 symptom types
- **Household & cosmetics tracking** — 14 categories (shampoo, dish soap, creams, toothpaste, etc.) with ingredient label scanning
- **Food photo recognition** — photograph a meal, AI identifies all dishes and products automatically
- **Ingredient label scanning** — photograph product labels, AI reads INCI list and flags allergens (SLS, MI/MCI, parabens, EU-26 fragrances, formaldehyde donors, and 35+ others)
- **AI allergy analysis** — finds correlations between food/medications/cosmetics and symptoms with probability scores and cross-reactivity detection
- **Allergenicity scoring** — rate any food product 0–100% using WHO/EAACI data
- **Calendar view** — browse diary by date, quick summary cards

## AI Models (via OpenRouter)

| Feature | Model |
|---|---|
| Food photo recognition | `google/gemma-3-4b-it:free` |
| Allergy pattern analysis | `google/gemini-3.1-flash-lite-preview` |
| Ingredient label scan | `google/gemini-3.1-flash-lite-preview` |

## Setup

1. Clone the repo
2. Open in Android Studio
3. Get an API key from [openrouter.ai](https://openrouter.ai)
4. Launch the app → Settings → enter your OpenRouter API key
5. Start logging your diary

## Tech Stack

- Kotlin + Jetpack Compose + Material3
- MVVM architecture with Hilt DI
- Room database
- Retrofit + OkHttp
- DataStore Preferences
- ExifInterface (photo orientation correction)

## Building

```bash
./gradlew assembleDebug
```

### CI Requirements

Add `AI_API_KEY` to your GitHub repository secrets (your OpenRouter API key). The CI workflow reads it from `secrets.AI_API_KEY`.

## Disclaimer

This app is for informational purposes only. AI analysis does not replace consultation with a physician or allergist.
