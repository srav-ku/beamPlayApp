package app.cinephile.ui.settings

import androidx.compose.runtime.Composable

/**
 * Full subtitle style editor with a live preview.
 *
 * Lives outside the player on purpose: the style is a device-wide preference
 * that applies to every video until it is changed, not a per-video setting.
 */
@Composable
expect fun BeamSubtitleSettingsScreen()