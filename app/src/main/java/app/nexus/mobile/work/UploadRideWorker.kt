package app.nexus.mobile.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.nexus.mobile.data.source.local.LocalRideLog
import app.nexus.mobile.data.source.remote.AcceptedRideDto
import app.nexus.mobile.data.source.remote.ApiClient
import java.util.concurrent.TimeUnit

/**
 * Lê o log local e tenta enviar todas as corridas pendentes.
 * Em caso de falha, retorna `Result.retry()` — o WorkManager aplica
 * backoff exponencial automaticamente.
 */
class UploadRideWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val log = LocalRideLog(applicationContext)
        val pending = log.readAll()
        if (pending.isEmpty()) return Result.success()

        val sent = mutableListOf<AcceptedRideDto>()
        for (dto in pending) {
            val ok = runCatching { ApiClient.nexusApi.postAcceptedRide(dto) }.isSuccess
            if (ok) sent += dto else break
        }

        log.removeAll(sent)

        return when {
            sent.size == pending.size -> Result.success()
            sent.isNotEmpty() -> Result.success() // parcial conta como progresso
            else -> Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "nexus_upload_rides"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<UploadRideWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        }
    }
}
