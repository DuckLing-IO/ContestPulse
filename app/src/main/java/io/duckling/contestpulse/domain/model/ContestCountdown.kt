package io.duckling.contestpulse.domain.model

sealed interface ContestCountdown {
    data object Running : ContestCountdown
    data object Finished : ContestCountdown
    data object Unknown : ContestCountdown
    data class Days(val value: Long) : ContestCountdown
    data class HoursMinutes(val hours: Long, val minutes: Long) : ContestCountdown
}
