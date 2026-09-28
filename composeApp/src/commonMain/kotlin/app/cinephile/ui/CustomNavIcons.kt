package app.cinephile.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Premium custom vector icons crafted for Cinephile's bottom navigation.
 */
object CustomNavIcons {
    private fun p(d: String) = addPathNodes(d)

    // Sleek Modern Home (Modern iOS geometry)
    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "NavHome",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = p("M3 10.182L10.364 3.763C11.31 2.936 12.69 2.936 13.636 3.763L21 10.182V19C21 20.105 20.105 21 19 21H15C14.448 21 14 20.552 14 20V15C14 13.895 13.105 13 12 13C10.895 13 10 13.895 10 15V20C10 20.552 9.552 21 9 21H5C3.895 21 3 20.105 3 19V10.182Z"),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }.build()
    }

    // Sleek Compass / Explore (Premium Browse icon)
    val Browse: ImageVector by lazy {
        ImageVector.Builder(
            name = "NavBrowse",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = p("M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z"),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
            )
            addPath(
                pathData = p("M16.24 7.76L14.12 14.12L7.76 16.24L9.88 9.88L16.24 7.76Z"),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }.build()
    }

    // Modern Bookmark / Grid Stack (Premium Library collections icon)
    val Library: ImageVector by lazy {
        ImageVector.Builder(
            name = "NavLibrary",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = p("M4 6C4 4.89543 4.89543 4 6 4H18C19.1046 4 20 4.89543 20 6V20L12 15.5L4 20V6Z"),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            addPath(
                pathData = p("M9 9H15M9 12H13"),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
            )
        }.build()
    }

    // Sleek Modern User Silhouette (Premium Profile icon)
    val Profile: ImageVector by lazy {
        ImageVector.Builder(
            name = "NavProfile",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = p("M12 11C14.2091 11 16 9.20914 16 7C16 4.79086 14.2091 3 12 3C9.79086 3 8 4.79086 8 7C8 9.20914 9.79086 11 12 11Z"),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
            )
            addPath(
                pathData = p("M6 21V19C6 16.7909 7.79086 15 10 15H14C16.2091 15 18 16.7909 18 19V21"),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }.build()
    }
}
