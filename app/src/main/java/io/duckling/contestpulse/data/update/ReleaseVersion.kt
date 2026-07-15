package io.duckling.contestpulse.data.update

/** Numeric release versions used by both the APK's versionName and GitHub tag. */
data class ReleaseVersion(
    private val major: Int,
    private val minor: Int,
    private val patch: Int,
) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int = compareValuesBy(
        this,
        other,
        ReleaseVersion::major,
        ReleaseVersion::minor,
        ReleaseVersion::patch,
    )

    companion object {
        private val versionPattern = Regex(
            pattern = "^v?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:[-+].*)?$",
            option = RegexOption.IGNORE_CASE,
        )

        fun parse(value: String): ReleaseVersion? {
            val match = versionPattern.matchEntire(value.trim()) ?: return null
            return ReleaseVersion(
                major = match.groupValues[1].toIntOrNull() ?: return null,
                minor = match.groupValues[2].ifEmpty { "0" }.toIntOrNull() ?: return null,
                patch = match.groupValues[3].ifEmpty { "0" }.toIntOrNull() ?: return null,
            )
        }
    }
}
