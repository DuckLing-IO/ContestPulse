package io.duckling.contestpulse.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.duckling.contestpulse.domain.reminder.ReminderManager
import io.duckling.contestpulse.reminder.AndroidAlarmReminderScheduler
import io.duckling.contestpulse.reminder.DefaultReminderManager
import io.duckling.contestpulse.reminder.ReminderScheduler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {
    @Binds
    @Singleton
    abstract fun bindReminderScheduler(
        scheduler: AndroidAlarmReminderScheduler,
    ): ReminderScheduler

    @Binds
    @Singleton
    abstract fun bindReminderManager(
        manager: DefaultReminderManager,
    ): ReminderManager
}
