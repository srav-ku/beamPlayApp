package app.cinephile.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/* ------------------------------------------------------------------ */
/*  TMDB person + combined credits. Fetched directly from the device.  */
/* ------------------------------------------------------------------ */

@Serializable
data class TmdbPerson(
    val id: Long = 0,
    val name: String = "",
    val biography: String? = null,
    val birthday: String? = null,
    val deathday: String? = null,
    val place_of_birth: String? = null,
    val known_for_department: String? = null,
    val profile_path: String? = null,
    val combined_credits: TmdbPersonCredits? = null,
)

@Serializable
data class TmdbPersonCredits(
    val cast: List<PersonCredit> = emptyList(),
    val crew: List<PersonCredit> = emptyList(),
)

/**
 * One entry of a person's filmography. The same shape covers both sides of the
 * credits endpoint, so a single card can show either a character (acting) or a
 * job (directing, writing) - that difference is what tells the user why the
 * title is on this person's list.
 */
@Serializable
data class PersonCredit(
    val id: Long = 0,
    val media_type: String? = null,
    val title: String? = null,
    val name: String? = null,
    val character: String? = null,
    val job: String? = null,
    val department: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val release_date: String? = null,
    val first_air_date: String? = null,
    val vote_average: Double? = null,
    val popularity: Double? = null,
) {
    val displayTitle: String get() = title ?: name ?: ""
    val year: Int? get() = (release_date ?: first_air_date)?.take(4)?.toIntOrNull()
    val isTv: Boolean get() = media_type == "tv"

    /** Character when acting, otherwise the job - never both. */
    val role: String? get() = character?.takeIf { it.isNotBlank() } ?: job?.takeIf { it.isNotBlank() }
}
/**
 * TMDB returns far more fields than the app cares about, so this decoder
 * ignores anything undeclared instead of failing the whole payload.
 */
val PersonJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
