package app.cinephile.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.GeistMono
import app.cinephile.core.ui.theme.PillShape

/** Filter values exactly as the backend returns them (never hardcoded). */
data class FilterOptionsUi(
    val genres: List<String> = emptyList(),
    val years: List<Int> = emptyList(),
    val languages: List<String> = emptyList(),
)

/**
 * Collapsible filter bar. The genre / year / language lists come from
 * `GET /filters`, so they only ever contain values that actually exist in the
 * database, matching the website's `CollapsibleFilterBar`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogFilterBar(
    options: FilterOptionsUi,
    activeGenre: String,
    activeYear: String,
    activeLanguage: String,
    onGenreChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Beam.colors
    var open by remember { mutableStateOf(false) }
    val activeCount = listOf(activeGenre, activeYear, activeLanguage).count { it.isNotBlank() }
    val chevronRotation by animateFloatAsState(if (open) 180f else 0f, label = "chevron")

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (open) colors.foreground else colors.card)
                    .border(1.dp, if (open) colors.foreground else colors.border, PillShape)
                    .clickable { open = !open }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = if (open) colors.background else colors.foreground,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = "Filter",
                    color = if (open) colors.background else colors.foreground,
                    fontFamily = GeistMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (activeCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .clip(CircleShape)
                            .background(if (open) colors.background else colors.foreground),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = activeCount.toString(),
                            color = if (open) colors.foreground else colors.background,
                            fontFamily = GeistMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (open) colors.background else colors.foreground,
                    modifier = Modifier.size(16.dp).rotate(chevronRotation),
                )
            }
        }

        AnimatedVisibility(
            visible = open,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.card)
                    .border(1.dp, colors.borderLight, RoundedCornerShape(18.dp))
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Filters",
                        color = colors.foreground,
                        fontFamily = GeistMono,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (activeCount > 0) {
                        Row(
                            modifier = Modifier
                                .clip(PillShape)
                                .clickable(onClick = onClear)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = colors.mutedForeground,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = "Clear all",
                                color = colors.mutedForeground,
                                fontFamily = GeistMono,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }

                FilterSection(
                    label = "Genre",
                    values = options.genres,
                    active = activeGenre,
                    onChange = onGenreChange,
                )
                FilterSection(
                    label = "Year",
                    values = options.years.map { it.toString() },
                    active = activeYear,
                    onChange = onYearChange,
                )
                FilterSection(
                    label = "Language",
                    values = options.languages,
                    active = activeLanguage,
                    onChange = onLanguageChange,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    label: String,
    values: List<String>,
    active: String,
    onChange: (String) -> Unit,
) {
    if (values.isEmpty()) return
    val colors = Beam.colors

    Column(Modifier.padding(top = 14.dp)) {
        Text(
            text = label.uppercase(),
            color = colors.mutedForeground,
            fontFamily = GeistMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterPill(text = "Any", selected = active.isBlank()) { onChange("") }
            values.forEach { value ->
                FilterPill(text = value, selected = active == value) { onChange(value) }
            }
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = Beam.colors

    Text(
        text = text,
        color = if (selected) colors.background else colors.mutedForeground,
        fontFamily = GeistMono,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        modifier = Modifier
            .clip(PillShape)
            .background(if (selected) colors.foreground else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) colors.foreground else colors.borderLight,
                shape = PillShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
