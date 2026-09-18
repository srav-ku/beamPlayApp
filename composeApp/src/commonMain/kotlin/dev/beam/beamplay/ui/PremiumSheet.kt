package dev.beam.beamplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.beam.beamplay.core.ui.theme.GeistMono
import dev.beam.beamplay.data.EntitlementsState

/**
 * Premium sheet, shown in place of the source picker when streaming is gated.
 *
 * The copy, the plan cards and the prices all come from the server payload, so the
 * paywall can be reworded or repriced without shipping a new APK. The fallback list
 * below is used only before the first successful fetch.
 */
private val FallbackBenefits = listOf(
    Triple("Stream instantly", "Every movie and series, in your browser. Subtitles, audio choices, no waiting.", "\u25B6"),
    Triple("Full-speed downloads", "Direct downloads at the fastest speed your connection can take.", "\u2193"),
    Triple("Zero ads", "Nothing between you and the film. Anywhere, on any device.", "\u2713"),
)

@Composable
fun PremiumSheet(onDismiss: () -> Unit) {
    val state = EntitlementsState.current
    val benefits = state?.benefits?.takeIf { it.isNotEmpty() }
        ?.map { Triple(it.name, it.description.orEmpty(), it.icon.orEmpty()) }
        ?: FallbackBenefits
    val plans = state?.plans.orEmpty()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Color(0xFF141418))
                .clickable(enabled = false) {}
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 28.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("BeamBot Premium", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Streaming is a premium feature. Downloads through Telegram stay free.",
                        color = Color(0xFF8F8F9A),
                        fontSize = 12.5.sp,
                    )
                }
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, "Close", tint = Color(0xFFAAAAAA), modifier = Modifier.size(15.dp))
                }
            }

            Spacer(Modifier.height(18.dp))

            benefits.forEach { (name, description, icon) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x21F5A623)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(icon, color = Color(0xFFF5A623), fontSize = 15.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                        if (description.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(description, color = Color(0xFF8F8F9A), fontSize = 12.5.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            if (plans.isNotEmpty()) {
                plans.forEach { plan ->
                    val featured = plan.isFeatured == 1
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (featured) Color(0x14F5A623) else Color(0xFF1B1B21))
                            .padding(16.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(plan.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(
                                text = if (plan.priceCents == 0) "Free" else "${plan.currency} ${plan.priceMajor}",
                                color = if (featured) Color(0xFFF5A623) else Color(0xFFEDEDED),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        plan.tagline?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, color = Color(0xFF8F8F9A), fontSize = 12.5.sp)
                        }
                        val on = plan.includes.filterValues { it }.keys
                        if (on.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = on.joinToString("  \u00B7  ") { key ->
                                    when (key) {
                                        "stream_instant" -> "Stream instantly"
                                        "download_full_speed" -> "Full-speed downloads"
                                        "no_ads" -> "Zero ads"
                                        else -> key
                                    }
                                },
                                color = Color(0xFF8F8F9A),
                                fontSize = 11.5.sp,
                                fontFamily = GeistMono,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1B1B21))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("HOW TO ACTIVATE", color = Color(0xFF8F8F9A), fontSize = 10.5.sp, fontFamily = GeistMono)
                listOf(
                    "Pay on the support page - it opens outside the app.",
                    "Come back here and open Profile \u2192 Plans.",
                    "Enter the reference or email you paid with.",
                ).forEachIndexed { index, line ->
                    Text("${index + 1}.  $line", color = Color(0xFFD7D7DD), fontSize = 12.5.sp)
                }
                Spacer(Modifier.height(2.dp))
                Text("Nothing is charged inside the app.", color = Color(0xFF8F8F9A), fontSize = 11.5.sp)
            }
        }
    }
}