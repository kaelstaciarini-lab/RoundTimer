package com.roundtimer.app

import android.app.Application
import androidx.room.Room
import com.roundtimer.app.data.db.AppDatabase
import com.roundtimer.app.data.settings.SettingsRepository

/** Container de dependências simples (sem framework de DI). */
class AppContainer(context: Application) {
    val database: AppDatabase = Room.databaseBuilder(
        context, AppDatabase::class.java, "round_timer.db"
    ).build()

    val settings: SettingsRepository = SettingsRepository(context)

    val presetDao get() = database.presetDao()
}

class RoundTimerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
