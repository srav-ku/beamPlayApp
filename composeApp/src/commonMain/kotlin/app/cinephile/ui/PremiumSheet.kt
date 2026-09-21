package app.cinephile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Block
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cinephile.core.model.PlanInfo
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.Fraunces
import app.cinephile.core.ui.theme.GeistMono
import app.cinephile.data.EntitlementsState
import androidx.compose.foundation.layout.fillMaxSize

/**
 * The paywall. One job: make the offer obvious and the next step obvious.
 *
 * Structure follows the app's own sheet language - card surface, top-corner
 * radius, a standalone close chip, amber primary - with the benefits condensed
 * into one card of three short rows instead of a paragraph each, and the
 * activation instructions folded into a single line under the button.
 */
@Composable
fun PremiumSheet(onDismiss: () -> Unit) {
    val colors = Beam.colors
    val state = EntitlementsState.current
    val plans = state?.plans.orEmpty().filter { it.priceCents > 0 }.sortedBy { it.priceCents }
    var selected by remember { mutableStateOf(plans.firstOrNull { it.isFeatured == 1 } ?: plans.firstOrNull()) }
    var showSteps by remember { mutableStateOf(false) }

    PlatformBackHandler(enabled = true) { onDismiss() }

    Box(Modifier.fillMaxSize()) {
        // Scrim
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(onClick = onDismiss),
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(colors.card)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                ) {}
                .navigationBarsPadding()
                .padding(24.dp),
        ) {
            Box(Modifier.fillMaxWidth()) {
                // Standalone close chip, its own corner rather than floating in copy.
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }

                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(8.dp))
                    Icon(PremiumCrown, contentDescription = null, tint = colors.amber500, modifier = Modifier.size(46.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Cinephile Premium",
                        color = colors.foreground,
                        fontFamily = Fraunces,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Stream. Download. Ad-free.",
                        color = colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))

                    // One benefits card, three short rows, hairline dividers.
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.card)
                            .border(1.dp, colors.border, RoundedCornerShape(18.dp)),
                    ) {
                        BenefitRow(Icons.Filled.PlayArrow, "Stream instantly", "No waiting, no buffering")
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
                        BenefitRow(Icons.Filled.Download, "Full-speed downloads", "Fastest speed your connection allows")
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
                        BenefitRow(Icons.Filled.Block, "Zero ads", "Nothing between you and the film")
                    }

                    Spacer(Modifier.height(20.dp))

                    // Prices come from the server; pick one, then upgrade.
                    if (plans.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            plans.forEach { plan ->
                                PlanChip(
                                    plan = plan,
                                    selected = selected?.id == plan.id,
                                    onClick = { selected = plan },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    Box(
                        Modifier
                            .width(220.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.amber500)
                            .clickable { showSteps = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Upgrade to Premium",
                            color = Color(0xFF101014),
                            fontFamily = GeistMono,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = if (showSteps && selected != null) {
                            "Pay " + selected!!.currency + " " + selected!!.priceMajor + " externally, then activate in Profile \u2192 Plans."
                        } else {
                            "Pay externally, activate in Profile \u2192 Plans"
                        },
                        color = colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, title: String, subtitle: String) {
    val colors = Beam.colors
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.amber500.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.amber500, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = colors.foreground, fontFamily = GeistMono, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun PlanChip(plan: PlanInfo, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Beam.colors
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.amber500.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, if (selected) colors.amber500.copy(alpha = 0.65f) else colors.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (plan.isFeatured == 1) {
            Text("BEST VALUE", color = colors.amber500, fontFamily = GeistMono, fontSize = 8.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(2.dp))
        }
        Text(plan.name, color = colors.foreground, fontFamily = GeistMono, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text(
            text = "\u20B9" + plan.priceMajor,
            color = if (selected) colors.amber500 else colors.foreground,
            fontFamily = GeistMono,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
