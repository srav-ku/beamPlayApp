package app.cinephile.core.model

import kotlinx.serialization.Serializable

/**
 * Everything the app needs to know about what is free and what is premium.
 *
 * Mirrors `GET /user/entitlements` on the worker. The app holds no premium rules of
 * its own: flip `stream_gate_enabled` in the database and this payload changes on the
 * next launch, with no APK update.
 */
@Serializable
data class EntitlementFlags(
    @kotlinx.serialization.SerialName("stream_gate_enabled") val streamGateEnabled: Boolean = false,
    @kotlinx.serialization.SerialName("ads_enabled") val adsEnabled: Boolean = false,
    @kotlinx.serialization.SerialName("pricing_screen") val pricingScreen: Boolean = true,
    @kotlinx.serialization.SerialName("enforce_device_limit") val enforceDeviceLimit: Boolean = false,
)

@Serializable
data class PlanInfo(
    val id: Long? = null,
    val code: String = "free",
    val name: String = "Free",
    val tagline: String? = null,
    @kotlinx.serialization.SerialName("price_cents") val priceCents: Int = 0,
    val currency: String = "INR",
    @kotlinx.serialization.SerialName("billing_period") val billingPeriod: String? = null,
    @kotlinx.serialization.SerialName("is_featured") val isFeatured: Int = 0,
    @kotlinx.serialization.SerialName("max_devices") val maxDevices: Int? = null,
    @kotlinx.serialization.SerialName("max_streams") val maxStreams: Int? = null,
    val status: String? = null,
    @kotlinx.serialization.SerialName("current_period_end") val currentPeriodEnd: Long? = null,
    val includes: Map<String, Boolean> = emptyMap(),
) {
    /** "149" from 14900 - avoids any currency formatting library. */
    val priceMajor: String get() = if (priceCents % 100 == 0) (priceCents / 100).toString() else String.format("%.2f", priceCents / 100.0)
    val isCurrent: Boolean get() = status == "active" || status == "grace"
}

@Serializable
data class FeatureBenefit(
    val key: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
)

@Serializable
data class AdsState(val show: Boolean = false)

@Serializable
data class Entitlements(
    val flags: EntitlementFlags = EntitlementFlags(),
    val plan: PlanInfo? = null,
    @kotlinx.serialization.SerialName("is_owner") val isOwner: Boolean = false,
    /** feature key -> enabled. Server-driven; the app never hardcodes a feature name's meaning. */
    val features: Map<String, Boolean> = emptyMap(),
    val ads: AdsState = AdsState(),
    val benefits: List<FeatureBenefit> = emptyList(),
    val plans: List<PlanInfo> = emptyList(),
) {
    fun has(feature: String): Boolean = isOwner || features[feature] == true

    /** Streaming is allowed unless the gate is on AND this user lacks the entitlement. */
    val canStream: Boolean get() = !flags.streamGateEnabled || has("stream_instant")

    /** Direct, full-speed in-app download. Telegram download stays free for everyone. */
    val canDirectDownload: Boolean get() = !flags.streamGateEnabled || has("download_full_speed")

    /** True only when the gate is actually on - used to decide whether to show a lock at all. */
    val streamLockedForThisUser: Boolean get() = flags.streamGateEnabled && !canStream
}