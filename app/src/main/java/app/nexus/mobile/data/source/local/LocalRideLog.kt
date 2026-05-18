package app.nexus.mobile.data.source.local

import android.content.Context
import com.google.gson.Gson
import app.nexus.mobile.data.source.remote.AcceptedRideDto
import java.io.File

/**
 * Log local de corridas aceitas (uma linha JSON por corrida).
 *
 * Funciona como:
 *  - Histórico para a UI quando não houver backend.
 *  - Buffer offline: corridas que falharem no envio são reenviadas
 *    pelo `UploadRideWorker` lendo este mesmo arquivo.
 */
class LocalRideLog(context: Context) {

    private val file: File = File(context.filesDir, "accepted_rides.jsonl")
    private val gson = Gson()

    @Synchronized
    fun append(ride: AcceptedRideDto) {
        file.appendText(gson.toJson(ride) + "\n")
    }

    @Synchronized
    fun readAll(): List<AcceptedRideDto> {
        if (!file.exists()) return emptyList()
        return file.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull { runCatching { gson.fromJson(it, AcceptedRideDto::class.java) }.getOrNull() }
    }

    /** Apaga somente os DTOs já confirmados pelo servidor. */
    @Synchronized
    fun removeAll(toRemove: Collection<AcceptedRideDto>) {
        if (toRemove.isEmpty() || !file.exists()) return
        val keep = readAll().filterNot { dto ->
            toRemove.any { it.acceptedAtEpochMs == dto.acceptedAtEpochMs && it.driverId == dto.driverId }
        }
        file.writeText(keep.joinToString("\n") { gson.toJson(it) } + if (keep.isNotEmpty()) "\n" else "")
    }
}
