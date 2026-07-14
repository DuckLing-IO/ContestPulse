package io.duckling.contestpulse.domain.model

data class ContestFilter(
    val sources: Set<ContestSource> = ContestSource.entries.toSet(),
    val favoriteOnly: Boolean = false,
    val ratedOnly: Boolean = false,
    val dateRange: ContestDateRange = ContestDateRange.ALL,
)

enum class ContestDateRange {
    ALL,
    NEXT_7_DAYS,
    NEXT_30_DAYS,
}
