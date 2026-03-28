package com.allergia.api

/**
 * Centralized model IDs used across the app.
 * Change here to switch models app-wide.
 *
 * Available on OpenRouter (verify at openrouter.ai/models):
 *   google/gemini-2.0-flash-lite   — fast, multimodal, very cheap, no free-tier rate limits
 *   google/gemini-2.5-flash-preview — fast, multimodal, good reasoning
 *   google/gemini-2.5-pro-preview   — strongest Google model, best analysis
 *   google/gemma-3-4b-it:free       — free tier only, strict rate limits (429 frequent)
 */
object Models {
    /** Food photo recognition (vision model) */
    const val FOOD_PHOTO = "google/gemini-2.0-flash-lite"

    /** Allergy dependency analysis + allergenicity search + product scoring */
    const val ANALYSIS = "google/gemini-2.0-flash-lite"

    /** Cosmetic ingredient label recognition (OCR-level accuracy required) */
    const val LABEL_SCAN = "google/gemini-2.0-flash-lite"
}

