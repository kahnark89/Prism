package com.cappsconsulting.prism.parentsuite.data

/**
 * The parent's operational configuration for their child — Doc 2.2 §5's settings
 * surface. Minimal by design: a child name (surfaced in the Companion's greeting)
 * and the active Companion device ID.
 *
 * Intentionally NOT a behavioral or biometric summary of the child — this is
 * configuration, not a profile the child would ever object to.
 */
data class ChildProfile(
    val childName: String = "",
    val activeCompanionId: String = "",
)
