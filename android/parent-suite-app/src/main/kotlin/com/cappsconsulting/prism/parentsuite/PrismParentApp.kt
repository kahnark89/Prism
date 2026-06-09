package com.cappsconsulting.prism.parentsuite

import android.app.Application

/**
 * Referenced by `AndroidManifest.xml` (`android:name=".PrismParentApp"`) — exists
 * to satisfy that reference honestly rather than leave the manifest pointing at a
 * name Android would silently paper over with a default [Application] instance.
 * Deliberately minimal: the actual composition root (wiring the sync link,
 * DataStore persistence, and the rest of the dependency graph) is real engineering
 * work gated on the sync transport and local storage layers that don't yet exist
 * in this port. Building that root before those pieces exist would be exactly the
 * "looks like it's working" trap this port names and routes around — see the
 * Companion's [com.cappsconsulting.prism.companion.PrismCompanionApp] for the
 * same reasoning applied to its own side.
 */
class PrismParentApp : Application()
