package com.allergia.api

/**
 * Centralized model IDs used across the app.
 * Change here to switch models app-wide.
 *
 * Available on OpenRouter (verify at openrouter.ai/models):
 *   google/gemini-2.5-flash-preview   — fast, multimodal, good reasoning
 *   google/gemini-2.5-pro-preview     — strongest Google model, best analysis
 *   google/gemma-3-4b-it:free         — free, fast, multimodal, food photo
 */
object Models {
    /** Food photo recognition (free tier, Gemma 3 vision) */
    const val FOOD_PHOTO = "google/gemma-3-4b-it:free"

    /** Allergy dependency analysis + allergenicity search + product scoring */
    const val ANALYSIS = "google/gemini-3.1-flash-lite-preview"

    /** Cosmetic ingredient label recognition (OCR-level accuracy required) */
    const val LABEL_SCAN = "google/gemini-3.1-flash-lite-preview"
}
