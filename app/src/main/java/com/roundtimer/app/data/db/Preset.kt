package com.roundtimer.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.roundtimer.app.timer.TimerConfig

@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val rounds: Int,
    val roundSec: Int,
    val restSec: Int,
    val prepareSec: Int,
) {
    fun toConfig() = TimerConfig(
        rounds = rounds,
        roundDurationSec = roundSec,
        restDurationSec = restSec,
        prepareDurationSec = prepareSec,
    )

    companion object {
        fun from(name: String, config: TimerConfig) = Preset(
            name = name,
            rounds = config.rounds,
            roundSec = config.roundDurationSec,
            restSec = config.restDurationSec,
            prepareSec = config.prepareDurationSec,
        )
    }
}
