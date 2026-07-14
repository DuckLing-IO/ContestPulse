package io.duckling.contestpulse.domain.model

enum class ContestGroupType {
    RUNNING,
    TODAY,
    TOMORROW,
    THIS_WEEK,
    LATER,
}

data class ContestGroup(
    val type: ContestGroupType,
    val contests: List<Contest>,
)
