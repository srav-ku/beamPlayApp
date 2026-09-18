package dev.beam.beamplay.ui

import androidx.compose.runtime.Composable

/**
 * System back button, expressed in common code.
 *
 * `enabled` decides whether this screen consumes the press. When nothing is open we leave it
 * disabled so the platform default applies and the app can actually be exited - otherwise the
 * back button would become dead on the last screen.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)