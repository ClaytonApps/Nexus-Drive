package com.nexusdrive.app.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Canal global para a tela de Debug observar o que o serviço de
 * acessibilidade está capturando em tempo real.
 *
 * Em release, o `DebugActivity` não fica acessível pela UI principal,
 * então este bus só consome memória se o usuário entrar nela.
 */
object CapturedTextBus {

    data class Snapshot(
        val packageName: String,
        val rawTexts: List<String>,
        val parsedPriceBrl: Double?,
        val parsedDistanceKm: Double?,
        val parsedDurationMin: Int?,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _flow = MutableSharedFlow<Snapshot>(
        replay = 1,
        extraBufferCapacity = 8
    )
    val flow: SharedFlow<Snapshot> = _flow

    fun emit(snapshot: Snapshot) {
        _flow.tryEmit(snapshot)
    }
}
