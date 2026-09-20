package app.cinephile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.Fraunces
import app.cinephile.core.ui.theme.GeistMono
import app.cinephile.data.Api

/**
 * One-time interest picker, shown right after a new account is created.
 *
 * Why it exists: a personalised shelf is only honest when it comes from
 * something the user actually told us. Until they have watch history, these
 * picks are the signal. The genres come from the catalogue, never hardcoded.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterestsCard(onDone: () -> Unit) {
    val colors = Beam.colors
    var options by remember { mutableStateOf<List<String>>(emptyList()) }
    var picked by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        options = runCatching { Api.getFilterOptions().genres }.getOrDefault(emptyList())
    }

    val canContinue = picked.isNotEmpty()

    Box(
        Modifier.fillMaxSize().background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "What do you like to watch?",
                color = colors.foreground,
                fontFamily = Fraunces,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (picked.isEmpty()) {
                    "Pick a few genres - we will use them until we learn from what you actually watch."
                } else {
                    picked.size.toString() + " selected"
                },
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                options.take(24).forEach { genre ->
                    val on = picked.contains(genre)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (on) colors.amber500 else Color.Transparent)
                            .border(1.dp, if (on) Color.Transparent else colors.border, RoundedCornerShape(50))
                            .clickable {
                                picked = if (on) picked - genre else picked + genre
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = genre,
                            color = if (on) Color(0xFF101014) else colors.foreground,
                            fontFamily = GeistMono,
                            fontSize = 13.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (canContinue) colors.amber500 else colors.muted)
                    .clickable(enabled = canContinue) {
                        saveInterests(picked)
                        onDone()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (canContinue) "Continue" else "Pick at least one",
                    color = if (canContinue) Color(0xFF101014) else colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Skip for now",
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        saveInterests(emptyList())
                        onDone()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}