package app.nexus.mobile.data.repository

import android.content.Context
import app.nexus.mobile.data.model.ProfitEstimate
import app.nexus.mobile.data.source.local.LocalRideLog
import app.nexus.mobile.data.source.remote.AcceptedRideDto
import app.nexus.mobile.work.UploadRideWorker

/**
 * Recebe corridas aceitas. Política de durabilidade:
 *
 *  1. Sempre persiste no [LocalRideLog] (arquivo JSONL).
 *  2. Enfileira o [UploadRideWorker], que tenta enviar tudo o que
 *     houver pendente, com backoff exponencial em caso de falha.
 *
 * Resultado: se o backend estiver fora do ar, o usuário não perde
 * nada — o WorkManager retenta quando a rede voltar.
 */
class AcceptedRideRepository(private val context: Context) {

    private val log = LocalRideLog(context)

    fun submit(
        driverId: String,
        sourceApp: String,
        estimate: ProfitEstimate,
        costPerKmBrl: Double
    ) {
        val dto = AcceptedRideDto(
            driverId = driverId,
            sourceApp = sourceApp,
            rideValueBrl = estimate.grossRevenueBrl,
            totalDistanceKm = estimate.totalDistanceKm,
            estimatedDurationMin = estimate.totalDurationMin,
            costPerKmBrl = costPerKmBrl,
            netProfitBrl = estimate.netProfitBrl,
            netProfitPerHour = estimate.netProfitPerHour,
            netProfitPerKm = estimate.netProfitPerKm,
            acceptedAtEpochMs = System.currentTimeMillis()
        )
        log.append(dto)
        UploadRideWorker.enqueue(context)
    }

    /** Histórico local — útil para uma futura tela de relatório. */
    fun history(): List<AcceptedRideDto> = log.readAll()
}
