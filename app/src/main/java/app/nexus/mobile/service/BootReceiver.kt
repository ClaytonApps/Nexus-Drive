package app.nexus.mobile.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.nexus.mobile.work.UploadRideWorker

/**
 * Após boot, drena qualquer corrida pendente no log local.
 * O serviço de acessibilidade é religado pelo próprio sistema —
 * não precisamos (nem podemos) iniciá-lo manualmente.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("NexusBoot", "BOOT_COMPLETED — enfileirando upload de pendências")
            UploadRideWorker.enqueue(context)
        }
    }
}
