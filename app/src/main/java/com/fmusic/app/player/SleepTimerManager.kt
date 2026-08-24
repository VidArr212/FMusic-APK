package com.fmusic.app.player

import android.os.CountDownTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SleepTimerManager(private val onTimerFinished: () -> Unit) {

    private var countDownTimer: CountDownTimer? = null

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    /**
     * Start sleep timer in minutes (min 5, max 30)
     */
    fun startTimer(minutes: Int) {
        cancelTimer()
        val clampedMinutes = minutes.coerceIn(5, 30)
        val totalMillis = clampedMinutes * 60 * 1000L

        _isActive.value = true
        _remainingSeconds.value = clampedMinutes * 60

        countDownTimer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingSeconds.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                _isActive.value = false
                _remainingSeconds.value = 0
                onTimerFinished()
            }
        }.start()
    }

    fun cancelTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        _isActive.value = false
        _remainingSeconds.value = 0
    }
}
