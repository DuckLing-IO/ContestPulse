package io.duckling.contestpulse.core.time

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.isActive

@Singleton
class MinuteTicker @Inject constructor(
    private val clock: Clock,
) {
    private val refreshEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun refresh() {
        refreshEvents.tryEmit(Unit)
    }

    fun stream(): Flow<Instant> = merge(
        periodicStream(),
        refreshEvents.map { clock.instant() },
    )

    private fun periodicStream(): Flow<Instant> = flow {
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
