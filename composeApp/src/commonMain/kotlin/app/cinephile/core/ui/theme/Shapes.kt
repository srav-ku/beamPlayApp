package app.cinephile.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii mirror the website's tokens:
 * `--card-radius: 26px`, `--pill-radius: 9999px`.
 */
val BeamShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp), // poster card
    medium = RoundedCornerShape(16.dp), // trending card
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp), // big card / sheet
)

/** Fully rounded pill (`--pill-radius`). */
val PillShape = RoundedCornerShape(50)
