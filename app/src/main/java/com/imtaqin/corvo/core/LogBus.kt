package com.imtaqin.corvo.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-wide log sink. The ViewModel writes here, and both the in-app Logs
 * screen and the floating-overlay service read from the same [StateFlow] — so
 * the overlay keeps showing live output even while Corvo is in the background.
 */
object LogBus {

    private const val MAX = 2000

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    fun append(lines: List<String>) {
        if (lines.isEmpty()) return
        _log.update { (it + lines).takeLast(MAX) }
    }

    fun clear() {
        _log.value = emptyList()
    }
}
