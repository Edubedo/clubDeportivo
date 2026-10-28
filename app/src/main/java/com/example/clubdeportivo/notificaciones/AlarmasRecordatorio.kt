package com.example.clubdeportivo.notificaciones

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.clubdeportivo.data.model.Reserva
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date

/**
 * La revisión periódica de Android (WorkManager) puede atrasarse bastante con el teléfono en reposo, y el aviso de una
 * reserva llegaba cuando ya estaba empezando. Por eso, para cada aviso de reserva (2 h y 1 h antes) se programa una
 * alarma que despierta la revisión justo a esa hora. Si la reserva se cancela, la alarma igual suena pero la revisión
 * no encuentra nada que avisar.
 */
object AlarmasRecordatorio {
    /** Margen para que la revisión ya caiga dentro de la ventana del aviso. */
    private const val MARGEN_MS = 30_000L

    fun programar(contexto: Context, reservas: List<Reserva>, ahora: Date) {
        val alarmas = contexto.getSystemService(AlarmManager::class.java) ?: return
        val exactas = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmas.canScheduleExactAlarms()
        reservas.forEach { reserva ->
            ReglasRecordatorios.momentosDeAviso(reserva, ahora).forEach { momento ->
                val intent = Intent(contexto, AlarmaRecordatorioReceiver::class.java)
                val pendiente = PendingIntent.getBroadcast(
                    contexto, "${reserva.id}:$momento".hashCode(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val cuando = momento + MARGEN_MS
                runCatching {
                    if (exactas) alarmas.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pendiente)
                    else alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pendiente)
                }
            }
        }
    }
}

/** Al sonar una alarma de aviso, corre la revisión de recordatorios en ese momento. */
class AlarmaRecordatorioReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(25_000L) { runCatching { MotorRecordatorios.ejecutar(contexto.applicationContext) } }
            } finally {
                pendiente.finish()
            }
        }
    }
}
