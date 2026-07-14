package io.duckling.contestpulse.core.time

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

@Singleton
class MinuteTicker @Inject constructor(
    private val clock: Clock,
) {
    fun stream(): Flow<Instant> = flow {
        while (currentCoroutineContext().isActive) {
            val now = clock.instant()
            emit(now)
            val untilNextMinute = MILLIS_PER_MINUTE -
                (now.toEpochMilli() % MILLIS_PER_MINUTE)
            delay(untilNextMinute)
        }
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
