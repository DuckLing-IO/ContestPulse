package io.duckling.contestpulse.domain.logic

import io.duckling.contestpulse.domain.model.ReminderProductConfig
import io.duckling.contestpulse.domain.model.ReminderRule

fun formatReminder(rule: ReminderRule): String = when (rule) {
    is ReminderRule.Relative -> formatRelativeReminder(rule.offsetMinutes)
    is ReminderRule.FixedTime -> if (rule.dayOffset == 0) {
        "比赛当天 ${twoDigits(rule.hour)}:${twoDigits(rule.minute)}"
    } else {
        "比赛前 ${rule.dayOffset} 天 ${twoDigits(rule.hour)}:${twoDigits(rule.minute)}"
    }
}

fun formatRelativeReminder(totalMinutes: Int): String {
    if (totalMinutes == 0) return "比赛开始时"
    val days = totalMinutes / ReminderProductConfig.MINUTES_PER_DAY
    val hours = totalMinutes % ReminderProductConfig.MINUTES_PER_DAY /
        ReminderProductConfig.MINUTES_PER_HOUR
    val minutes = totalMinutes % ReminderProductConfig.MINUTES_PER_HOUR
    return buildList {
        if (days > 0) add("$days 天")
        if (hours > 0) add("$hours 小时")
        if (minutes > 0) add("$minutes 分钟")
    }.joinToString(separator = " ", prefix = "比赛前 ")
}

fun twoDigits(value: Int): String = value.toString().padStart(2, '0')
