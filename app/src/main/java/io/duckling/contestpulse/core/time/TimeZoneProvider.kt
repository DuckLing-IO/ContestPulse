package io.duckling.contestpulse.core.time

import java.time.ZoneId

fun interface TimeZoneProvider {
    fun currentZoneId(): ZoneId
}

class SystemTimeZoneProvider : TimeZoneProvider {
    override fun currentZoneId(): ZoneId = ZoneId.systemDefault()
}
