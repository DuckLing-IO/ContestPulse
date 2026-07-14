package io.duckling.contestpulse.core.calendar

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import io.duckling.contestpulse.R
import io.duckling.contestpulse.domain.model.Contest
import java.time.ZoneId

fun Context.addContestToSystemCalendar(contest: Contest): Boolean {
    val endTime = contest.endTime
        ?: contest.duration?.let(contest.startTime::plus)
        ?: contest.startTime
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, contest.startTime.toEpochMilli())
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.toEpochMilli())
        putExtra(CalendarContract.Events.TITLE, contest.title)
        putExtra(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
        putExtra(
            CalendarContract.Events.DESCRIPTION,
            getString(
                R.string.calendar_event_description,
                contest.platformDisplayName(),
                contest.contestUrl,
            ),
        )
    }
    return runCatching { startActivity(intent) }.isSuccess
}

private fun Contest.platformDisplayName(): String = when (source) {
    io.duckling.contestpulse.domain.model.ContestSource.CODEFORCES -> "Codeforces"
    io.duckling.contestpulse.domain.model.ContestSource.ATCODER -> "AtCoder"
    io.duckling.contestpulse.domain.model.ContestSource.LUOGU -> "Luogu"
    io.duckling.contestpulse.domain.model.ContestSource.NOWCODER -> "Nowcoder"
    io.duckling.contestpulse.domain.model.ContestSource.OTHER -> category ?: "Contest"
}
