package com.dermify.dermify.curation

/**
 * Consent terms are versioned. Every contribution records the version the user
 * actually agreed to, so changing the terms later does not retroactively
 * reinterpret what was already collected.
 *
 * When the wording below changes in any way that alters what the user is
 * agreeing to, bump [CURRENT_VERSION]. Never edit the text of a shipped
 * version in place.
 */
object ConsentPolicy {

    const val CURRENT_VERSION = "consent-v1-2026-08"

    /**
     * Shown in full on the consent screen. The withdrawal paragraph states the
     * limit plainly on purpose — an image already sealed into a published
     * dataset version cannot be pulled back out of a model that was trained on
     * it, and promising otherwise would be false.
     */
    const val CONSENT_TEXT: String =
        "Contributing this image is optional. You get your result either way.\n\n" +
        "If you contribute:\n" +
        "• The photo is shrunk and re-saved on this phone before it is sent. " +
        "Location data, camera details and the capture time are removed.\n" +
        "• Nothing that identifies you is attached — no name, no account ID, no " +
        "device ID. Only the date (not the time), the app's guess, and the " +
        "country.\n" +
        "• Photos containing a face are never contributed.\n" +
        "• A dermatologist may later label the image. It is used to train " +
        "future versions of Dermify.\n\n" +
        "You can withdraw a contribution from Settings at any time. The stored " +
        "image is deleted immediately. However, if it was already included in a " +
        "published training set, it cannot be removed from models that have " +
        "already been trained on it."

    /** Coarse region attached to a contribution. Country only — never GPS. */
    const val REGION = "GH"
}
